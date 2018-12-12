package com.epam.talks.github

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.epam.talks.github.model.RetrofitApiClientImpl
import kotlinx.android.synthetic.main.activity_repositories.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import java.lang.Exception

class RepositoriesActivity : AppCompatActivity() {

    val broadcast = ConflatedBroadcastChannel<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repositories)

        val reposNames = intent?.extras?.getStringArrayList("repos")
        repos.adapter = ReposAdapter(ArrayList(reposNames), this@RepositoriesActivity)

        val apiClient = RetrofitApiClientImpl()

        CoroutineScope(Dispatchers.Main).launch {
            broadcast.consumeEach { query ->
                delay(300)

                try {
                    val foundRepositories = apiClient.searchRepositories(query)
                    Log.d("ConflatedBroadcast", "Query = ${query}, response: ${foundRepositories?.size}")
                    repos.adapter = ReposAdapter(
                            foundRepositories!!.map { it.full_name },
                            this@RepositoriesActivity)
                } catch (e: Exception) {
                    Log.d("ConflatedBroadcast", e.message)
                }
            }
        }

        searchQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                broadcast.offer(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    class RepoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val name: TextView = view.findViewById(R.id.repoName)
        val stars: TextView = view.findViewById(R.id.repoStars)
    }

    class ReposAdapter(val reposNames: List<String>, val context: Context) : RecyclerView.Adapter<RepoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepoViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.repo_item, null)
            return RepoViewHolder(view)
        }

        override fun onBindViewHolder(holder: RepoViewHolder, position: Int) {
            holder.name.text = reposNames[position]
        }

        override fun getItemCount(): Int {
            return reposNames.count()
        }
    }
}

fun showRepositories(context: Context, repos: List<String>) {
    val intent = Intent(context, RepositoriesActivity::class.java)
    intent.putExtra("repos", ArrayList(repos))
    context.startActivity(intent)
}
