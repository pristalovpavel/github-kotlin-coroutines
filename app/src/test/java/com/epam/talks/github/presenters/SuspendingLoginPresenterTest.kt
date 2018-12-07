package com.epam.talks.github.presenters

import com.epam.talks.github.data.GithubRepository
import com.epam.talks.github.data.GithubUser
import com.epam.talks.github.model.SuspendingApiClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CommonPool
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.*

class SuspendingLoginPresenterTest {

	@Test
	fun testLogin() = runBlocking {
		val apiClient = mockk<SuspendingApiClient.SuspendingApiClientImpl>()
		val githubUser = GithubUser("login", 1, "url", "name")
		val repositories = GithubRepository(1, "repos_name", "full_repos_name")

		coEvery { apiClient.login(any()) } returns githubUser
		coEvery { apiClient.getRepositories(any(), any()) } returns Arrays.asList(repositories)

		val loginPresenterImpl = SuspendingLoginPresenterImpl(apiClient, CommonPool)
		runBlocking {
			val repos = loginPresenterImpl.doLogin("login", "password")
			assertNotNull(repos)
		}
	}
}
