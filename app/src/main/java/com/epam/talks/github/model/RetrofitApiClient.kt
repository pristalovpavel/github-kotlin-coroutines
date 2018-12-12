package com.epam.talks.github.model

import com.epam.talks.github.data.GithubRepository
import com.epam.talks.github.data.GithubUser
import com.epam.talks.github.data.RepoSearchResult
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.*
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.IOException


/**
 * @Author pristalov
 * Created 05 December 2018
 */

interface RetrofitApiClient {

    @GET("/user")
    fun loginAndGetUser(): Deferred<Response<GithubUser>>

    @GET("search/repositories?page=1")
    fun searchRepositories(@Query("q") query: String): Deferred<Response<RepoSearchResult>>

    @GET()
    fun getRepositories(@Url url: String): Deferred<Response<List<GithubRepository>>>

    companion object Factory {
        private lateinit var retrofit: Retrofit

        fun create(login: String = "", password: String = ""): RetrofitApiClient {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val clientBuilder = OkHttpClient.Builder()

            if (!login.isBlank() && !password.isBlank()) {
                clientBuilder.addInterceptor(BasicAuthInterceptor(login, password))
            }
            clientBuilder.addInterceptor(logging)

            retrofit = Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .client(clientBuilder.build())
                    .build()

            return retrofit.create(RetrofitApiClient::class.java)
        }
    }
}

class RetrofitApiClientImpl {

    suspend fun getUser(login: String, password: String): GithubUser? = withContext(Dispatchers.Default) {
            val response = RetrofitApiClient.create(login, password).loginAndGetUser().await()
            if (response.code() != 200) {
                throw RuntimeException("Incorrect login or password")
            }
            response.body()
    }

    suspend fun searchRepositories(query: String): List<GithubRepository>? = withContext(Dispatchers.Default) {
            val response = RetrofitApiClient.create().searchRepositories(query).await().body()
        return@withContext response?.items ?: ArrayList()
    }

    suspend fun getRepositories(url: String, login: String, password: String): List<GithubRepository>? = withContext(Dispatchers.Default) {
            RetrofitApiClient.create(login, password).getRepositories(url).await().body()
    }
}

class BasicAuthInterceptor(user: String, password: String) : Interceptor {

    private val credentials: String

    init {
        this.credentials = Credentials.basic(user, password)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build()
        return chain.proceed(authenticatedRequest)
    }
}