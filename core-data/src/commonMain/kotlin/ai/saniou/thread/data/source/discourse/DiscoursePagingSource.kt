package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.domain.model.forum.Post
import app.cash.paging.PagingSource
import app.cash.paging.PagingState

class DiscoursePagingSource(
    private val api: DiscourseApi,
    private val forumId: String
) : PagingSource<Int, Post>() {

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 0
        return try {
            val response = if (forumId == "latest") {
                api.getLatestTopics(page)
            } else {
                api.getCategoryTopics(forumId, page)
            }

            val usersMap = response.users.associateBy { it.id }
            val posts = response.topicList.topics.map { topic ->
                topic.toPost(usersMap)
            }

            // Discourse returns a list of topics. If we get fewer topics than requested or empty, we are done.
            // However, Discourse pagination logic usually depends on `more_topics_url` or similar flags.
            // But simple page increment usually works until no more topics are returned.
            // A common page size in Discourse is 30.
            val nextKey = if (posts.isEmpty()) null else page + 1
            
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 0) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}