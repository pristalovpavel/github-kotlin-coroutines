package com.epam.talks.github

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.epam.talks.github.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import khttp.structures.authorization.BasicAuthorization
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLoginRetrofit()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener {
            //attemptLogin()
            //attemptLoginRx()
            attemptLoginRetrofit()
        }
    }

    private fun attemptLoginRx() {
        val login = email.text.toString()
        val pass = password.text.toString()

        val auth = BasicAuthorization(login, pass)
        val apiClient = ApiClientRx.ApiClientRxImpl()
        showProgress(true)
        apiClient.login(auth)
                .flatMap { user ->
                    apiClient.getRepositories(user.repos_url, auth)
                }
                .map { list -> list.map { it.full_name } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { showProgress(false) }
                .subscribe(
                        { list -> showRepositories(this@LoginActivity, list) },
                        { error -> Log.e("TAG", "Failed to show repos", error) }
                )
    }

    private fun attemptLoginSuspending() {
        val login = email.text.toString()
        val pass = password.text.toString()
        val apiClient = SuspendingApiClient.SuspendingApiClientImpl()
        CoroutineScope(Dispatchers.Main).launch {
            showProgress(true)
            val auth = BasicAuthorization(login, pass)
            try {
                val userInfo = withContext(Dispatchers.IO) { apiClient.login(auth) }
                val repoUrl = userInfo.repos_url
                val list = withContext(Dispatchers.IO) { apiClient.getRepositories(repoUrl, auth) }
                showRepositories(this@LoginActivity, list.map { it -> it.full_name })
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message, LENGTH_LONG).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun attemptLogin() {
        val login = email.text.toString()
        val pass = password.text.toString()
        val apiClient = ApiClient.ApiClientImpl()
        CoroutineScope(Dispatchers.Main).launch {
            showProgress(true)
            val auth = BasicAuthorization(login, pass)
            try {
                val userInfo = apiClient.login(auth).await()
                if (!isActive) {
                    return@launch
                }
                val repoUrl = userInfo.repos_url
                val list = apiClient.getRepositories(repoUrl, auth).await()
                showRepositories(this@LoginActivity, list.map { it -> it.full_name })
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message, LENGTH_LONG).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun attemptLoginRetrofit() {
        val login = email.text.toString()
        val pass = password.text.toString()
        val apiClient = RetrofitApiClientImpl()

        showProgress(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userInfo = apiClient.getUser(login, pass)
                if(!isActive) return@launch

                val repoUrl = userInfo!!.repos_url
                val list = apiClient.getRepositories(repoUrl, login, pass)
                val repos = list!!.map { it -> it.full_name }
                showRepositories(this@LoginActivity, repos)
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message, LENGTH_LONG).show()
            } finally {
                showProgress(false)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            login_progress.visibility = View.VISIBLE
        } else {
            login_progress.visibility = View.GONE
        }
    }

}
