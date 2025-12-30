package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscoursePost
import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.network.SaniouResult
import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingState
import kotlin.time.Instant

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
        return when (val result = api.getTopic(threadId, page)) {
            is SaniouResult.Success -> {
                try {
                    val response = result.data
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
            is SaniouResult.Error -> {
                LoadResult.Error(result.ex)
            }
        }
    }
}

private fun DiscoursePost.toThreadReply(threadId: Long): ThreadReply {
    val replyCreatedAt = try {
        Instant.parse(createdAt)
    } catch (e: Exception) {
        Instant.fromEpochMilliseconds(0)
    }

    val author = Author(
        id = username,
        name = name ?: username,
        avatar = avatarTemplate // Assuming avatarTemplate is available or mapped elsewhere if needed
    )

    return ThreadReply(
        id = id.toString(),
        topicId = threadId.toString(),
        author = author,
        createdAt = kotlin.time.Instant.fromEpochMilliseconds(replyCreatedAt.toEpochMilliseconds()),
        title = "",
        content = cooked,
        images = emptyList(), // TODO: Extract images from cooked content or other fields
        isAdmin = false, // TODO: Map admin status from user_title or similar
        floor = postNumber,
        replyToId = replyToPostNumber?.toString()
    )
}
