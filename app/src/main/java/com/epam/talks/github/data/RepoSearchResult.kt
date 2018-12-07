package com.epam.talks.github.data

/**
 * @Author pristalov
 * Created 07 December 2018
 */

data class RepoSearchResult (
        val total_count: Int,
        val items: List<GithubRepository>
)
