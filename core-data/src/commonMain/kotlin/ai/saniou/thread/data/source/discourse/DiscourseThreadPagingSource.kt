package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscoursePost
import ai.saniou.thread.domain.model.forum.ThreadReply
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import kotlinx.datetime.Instant

class DiscourseThreadPagingSource(
    private val api: DiscourseApi,
    private val threadId: String
) : PagingSource<Int, ThreadReply>() {

    override fun getRefreshKey(state: PagingState<Int, ThreadReply>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ThreadReply> {
        val page = params.key ?: 1 // Discourse pages start at 1
        return try {
            val response = api.getTopic(threadId, page)

            val replies = response.postStream.posts.map { it.toThreadReply(response.id) }

            // Check if there are more posts.
            // post_stream.stream contains all IDs. If we have fetched all, we are done.
            // Or simpler: if replies are empty or fewer than page size (usually 20 for Discourse details).
            // But let's check `posts_count`.
            val postsCount = response.postsCount
            // We can calculate if next page exists.
            // But strict checking is complex because page size varies.
            // Simple heuristic: if we got posts, try next page.
            // Or better: check if the last post number is the highest post number?
            // Actually `post_stream` doesn't strictly guarantee full page in response if IDs are passed.
            // But with `page` param it should work like pagination.

            val nextKey = if (replies.isEmpty()) null else page + 1

            LoadResult.Page(
                data = replies,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

private fun DiscoursePost.toThreadReply(threadId: Long): ThreadReply {
    val replyCreatedAt = try {
        Instant.parse(createdAt)
    } catch (e: Exception) {
        Instant.fromEpochMilliseconds(0)
    }

    return ThreadReply(
        id = id,
        userHash = username,
        admin = 0,
        title = "",
        now = createdAt,
        content = cooked,
        img = "",
        ext = "",
        name = name ?: username,
        threadId = threadId,
    )
}
