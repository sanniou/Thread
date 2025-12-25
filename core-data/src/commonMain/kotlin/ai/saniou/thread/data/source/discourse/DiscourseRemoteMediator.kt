package ai.saniou.thread.data.source.discourse

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseLatestPostsResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset as GetThreadsInForumOffset
import ai.saniou.thread.db.table.forum.Topic as Thread
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult

@OptIn(ExperimentalPagingApi::class)
class DiscourseRemoteMediator(
    private val sourceId: String,
    private val fid: String,
    private val api: DiscourseApi,
    private val cache: SourceCache,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int = 0,
) : RemoteMediator<Int, GetThreadsInForumOffset>() {

    private val delegate =
        GenericRemoteMediator<Int, GetThreadsInForumOffset, DiscourseLatestPostsResponse>(
            db = db,
            dataPolicy = dataPolicy,
            initialKey = initialPage,
            remoteKeyStrategy = DefaultRemoteKeyStrategy(
                db = db,
                type = RemoteKeyType.FORUM,
                id = fid,
                itemIdExtractor = { it.channelId }
            ),
            fetcher = { page ->
                try {
                    val response = if (fid == "latest") {
                        api.getLatestTopics(page)
                    } else {
                        api.getCategoryTopics(fid, page)
                    }
                    SaniouResponse.Success(response)
                } catch (e: Exception) {
                    throw e
                }
            },
            saver = { response, page, loadType ->
                val usersMap = response.users.associateBy { it.id }
                val topics = response.topicList.topics
                val threads = topics.map { topic ->
                    topic.toThreadEntity(sourceId, usersMap, page.toLong())
                }

                if (loadType == LoadType.REFRESH) {
                    db.topicQueries.deleteTopicsByChannelAndPage(sourceId, fid, page.toLong())
                }
                threads.forEach { thread ->
                    db.topicQueries.upsertTopic(thread)
                    db.topicQueries.upsertTopicInformation(
                        id = thread.id,
                        sourceId = sourceId,
                        remainingCount = 0,
                        lastKey = 0 // Placeholder
                    )
                }
            },
            endOfPaginationReached = { response ->
                response.topicList.topics.isEmpty()
            },
            cacheChecker = { page ->
                val threadsInDb =
                    db.topicQueries.countTopicsByChannelAndPage(sourceId, fid, page.toLong())
                        .executeAsOne()
                threadsInDb > 0
            },
            keyIncrementer = { it + 1 },
            keyDecrementer = { it - 1 },
            keyToLong = { it.toLong() },
            longToKey = { it.toInt() }
        )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GetThreadsInForumOffset>,
    ): RemoteMediatorMediatorResult {
        return delegate.load(loadType, state)
    }
}

private fun DiscourseTopic.toThreadEntity(
    sourceId: String,
    usersMap: Map<Long, DiscourseUser>,
    page: Long,
): Thread {
    val originalPosterId =
        posters.firstOrNull { it.description.contains("Original Poster") }?.userId
            ?: posters.firstOrNull()?.userId
    val user = originalPosterId?.let { usersMap[it] }

    return Thread(
        id = id.toString(),
        sourceId = sourceId,
        channelId = categoryId.toString(),
        commentCount = replyCount.toLong(),
        // Image handled separately, set empty for now
        // img = imageUrl ?: "",
        // ext = "",
        createdAt = createdAt.toTime().epochSeconds,
        userHash = user?.username ?: "Unknown",
        authorName = user?.name ?: user?.username ?: "Anonymous",
        title = title,
        content = excerpt ?: fancyTitle,
        sage = 0,
        admin = if (pinned || closed) 1 else 0,
        hide = if (!visible) 1 else 0,
        page = page
    )
}
