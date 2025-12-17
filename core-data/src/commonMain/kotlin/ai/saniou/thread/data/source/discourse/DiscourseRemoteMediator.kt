package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.db.table.forum.GetThreadsInForumOffset
import ai.saniou.thread.db.table.forum.Thread
import ai.saniou.thread.db.table.forum.ThreadReply
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult
import app.cash.paging.RemoteMediatorMediatorResultError
import app.cash.paging.RemoteMediatorMediatorResultSuccess
import kotlinx.datetime.Instant

@OptIn(ExperimentalPagingApi::class)
class DiscourseRemoteMediator(
    private val sourceId: String,
    private val fid: String,
    private val api: DiscourseApi,
    private val cache: SourceCache,
    private val initialPage: Int = 0
) : RemoteMediator<Int, GetThreadsInForumOffset>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GetThreadsInForumOffset>
    ): RemoteMediatorMediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> initialPage
            LoadType.PREPEND -> return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                if (lastItem == null) {
                    initialPage
                } else {
                    // Discourse pagination logic might differ, assuming page based for now
                    // If last item exists, we need the next page.
                    // Since we don't store page number in Thread entity easily accessible here without query,
                    // we might need to rely on state.pages size or similar if we assume sequential loading.
                    // However, RemoteMediator is stateless regarding page numbers usually.
                    // A common pattern is to use RemoteKeys table.
                    // For simplicity in this first pass, let's assume we can calculate page from offset or similar,
                    // OR we just increment based on loaded count / page size.
                    (state.pages.size + initialPage)
                }
            }
        }

        return try {
            val response = if (fid == "latest") {
                api.getLatestTopics(page)
            } else {
                api.getCategoryTopics(fid, page)
            }

            val usersMap = response.users.associateBy { it.id }
            val topics = response.topicList.topics
            val endOfPaginationReached = topics.isEmpty()

            val threads = topics.map { topic ->
                topic.toThreadEntity(sourceId, usersMap, page.toLong())
            }

            if (loadType == LoadType.REFRESH) {
                cache.clearForumCache(sourceId, fid)
            }
            cache.saveThreads(threads)

            RemoteMediatorMediatorResultSuccess(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            RemoteMediatorMediatorResultError(e)
        }
    }
}

private fun DiscourseTopic.toThreadEntity(
    sourceId: String,
    usersMap: Map<Long, DiscourseUser>,
    page: Long
): Thread {
    val originalPosterId = posters.firstOrNull { it.description.contains("Original Poster") }?.userId
        ?: posters.firstOrNull()?.userId
    val user = originalPosterId?.let { usersMap[it] }

    return Thread(
        id = id.toString(),
        sourceId = sourceId,
        fid = categoryId.toString(),
        replyCount = replyCount.toLong(),
        img = imageUrl ?: "",
        ext = "",
        now = createdAt,
        userHash = user?.username ?: "Unknown",
        name = user?.name ?: user?.username ?: "Anonymous",
        title = title,
        content = excerpt ?: fancyTitle,
        sage = 0,
        admin = if (pinned || closed) 1 else 0,
        hide = if (!visible) 1 else 0,
        page = page
    )
}
