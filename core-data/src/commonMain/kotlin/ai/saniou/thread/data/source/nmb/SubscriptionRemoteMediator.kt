package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.Feed
import ai.saniou.thread.data.source.nmb.remote.dto.nowToEpochMilliseconds
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.SelectSubscriptionTopic
import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.network.toResult
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator

@OptIn(ExperimentalPagingApi::class)
class SubscriptionRemoteMediator(
    private val subscriptionKey: String,
    private val forumRepository: NmbXdApi,
    private val db: Database,
    private val dataPolicy: DataPolicy,
) : RemoteMediator<Int, SelectSubscriptionTopic>() {

    private val delegate = GenericRemoteMediator<SelectSubscriptionTopic, Feed>(
        db = db,
        dataPolicy = dataPolicy,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = "subscribe_$subscriptionKey",
            itemTargetIdExtractor = { feed -> feed.id }
        ),
        fetcher = { cursor ->
            val page = cursor?.toIntOrNull() ?: 1
            forumRepository.feed(subscriptionKey, page.toLong()).toResult().map { feeds ->
                val nextCursor = if (feeds.isNotEmpty()) (page + 1).toString() else null
                val prevCursor = if (page > 1) (page - 1).toString() else null
                PagedResult(feeds, prevCursor, nextCursor)
            }
        },
        saver = { feedDetail, loadType ->
            if (loadType == LoadType.REFRESH) {
                db.subscriptionQueries.deleteCloudSubscriptions(subscriptionKey)
            }

            // Infer page from DB count for APPEND, 1 for REFRESH
            val page = if (loadType == LoadType.REFRESH) 1L else {
                 (db.subscriptionQueries.countSubscriptionsBySubscriptionKey(subscriptionKey).executeAsOne() / 19) + 1
            }

            feedDetail.forEach { feed ->
                val topic = feed.toTable("nmb", db.topicQueries)
                db.topicQueries.upsertTopicNoPage(topic)

                db.subscriptionQueries.insertSubscription(
                    subscriptionKey = subscriptionKey,
                    sourceId = "nmb",
                    topicId = feed.id.toString(),
                    page = page,
                    subscriptionTime = feed.now.nowToEpochMilliseconds(),
                    isLocal = 0
                )
            }
        },
        itemTargetIdExtractor = { feed -> feed.id.toString() },
        cacheChecker = { cursor ->
            val page = cursor?.toIntOrNull() ?: 1
            val subscriptionsInDb = db.subscriptionQueries
                .countSubscriptionsBySubscriptionKeyAndPage(subscriptionKey, page.toLong())
                .executeAsOne()
            subscriptionsInDb > 0
        }
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SelectSubscriptionTopic>,
    ): MediatorResult {
        return delegate.load(loadType, state)
    }
}
