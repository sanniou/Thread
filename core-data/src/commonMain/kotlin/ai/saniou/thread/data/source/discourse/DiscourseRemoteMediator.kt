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
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.network.toResult
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.MediatorResult

@OptIn(ExperimentalPagingApi::class)
class DiscourseRemoteMediator(
    private val sourceId: String,
    private val fid: String,
    private val api: DiscourseApi,
    private val cache: SourceCache,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int = 0,
) : RemoteMediator<Int, GetTopicsInChannelOffset>() {

    private val delegate =
        GenericRemoteMediator<Int, GetTopicsInChannelOffset, DiscourseLatestPostsResponse>(
            db = db,
            dataPolicy = dataPolicy,
            initialKey = initialPage,
            remoteKeyStrategy = DefaultRemoteKeyStrategy(
                db = db,
                type = RemoteKeyType.CHANNEL,
                id = fid,
            ),
            fetcher = { page ->
                if (fid == "latest") {
                    api.getLatestTopics(page)
                } else {
                    api.getCategoryTopics(fid, page)
                }.toResult()
            },
            saver = { response, page, loadType ->
                val usersMap = response.users.associateBy { it.id }
                val discourseTopics = response.topicList.topics
                val topics = discourseTopics.map { topic ->
                    topic.toTopicEntity(sourceId, usersMap, page.toLong())
                }

                if (loadType == LoadType.REFRESH) {
                    db.topicQueries.deleteTopicsByChannelAndPage(sourceId, fid, page.toLong())
                }
                topics.forEach { topic ->
                    db.topicQueries.upsertTopic(topic)
                    db.topicQueries.upsertTopicInformation(
                        id = topic.id,
                        sourceId = sourceId,
                        remainingCount = 0,
                        lastReplyAt = 0 // Placeholder
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
        state: PagingState<Int, GetTopicsInChannelOffset>,
    ): MediatorResult {
        return delegate.load(loadType, state)
    }
}

private fun DiscourseTopic.toTopicEntity(
    sourceId: String,
    usersMap: Map<Long, DiscourseUser>,
    page: Long,
): Topic {
    val originalPosterId =
        posters.firstOrNull { it.description.contains("Original Poster") }?.userId
            ?: posters.firstOrNull()?.userId
    val user = originalPosterId?.let { usersMap[it] }

    return Topic(
        id = id.toString(),
        sourceId = sourceId,
        channelId = categoryId.toString(),
        commentCount = replyCount.toLong(),
        // Image handled separately, set empty for now
        // img = imageUrl ?: "",
        // ext = "",
        createdAt = createdAt.toTime().toEpochMilliseconds(),
        authorId = user?.username ?: "Unknown",
        authorName = user?.name ?: user?.username ?: "Anonymous",
        title = title,
        content = null, // LIST API does NOT return cooked content, set NULL to avoid overwriting existing cache
        summary = excerpt ?: fancyTitle,
        page = page,
        agreeCount = 0,
        disagreeCount = 0,
        isCollected = false,
        )
}
