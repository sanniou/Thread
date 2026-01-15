package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.Feed
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.nowToEpochMilliseconds
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.SelectSubscriptionTopic
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

    private val delegate = GenericRemoteMediator<Int, SelectSubscriptionTopic, Feed>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = 1,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = RemoteKeyType.SUBSCRIBE,
            id = subscriptionKey,
            serializer = { it.toString() },
            deserializer = { it.toInt() }
        ),
        fetcher = { page -> forumRepository.feed(subscriptionKey, page.toLong()).toResult() },
        saver = { feedDetail, page, loadType ->
            if (loadType == LoadType.REFRESH) {
                db.subscriptionQueries.deleteCloudSubscriptions(subscriptionKey)
            }

            feedDetail.forEach { feed ->
                val topic = feed.toTable("nmb", db.topicQueries)
                db.topicQueries.upsertTopicNoPage(topic)

                db.subscriptionQueries.insertSubscription(
                    subscriptionKey = subscriptionKey,
                    sourceId = "nmb",
                    topicId = feed.id.toString(),
                    page = page.toLong(),
                    subscriptionTime = feed.now.nowToEpochMilliseconds(),
                    isLocal = 0
                )
            }
        },
        endOfPaginationReached = { it.isEmpty() },
        cacheChecker = { page ->
            val subscriptionsInDb = db.subscriptionQueries
                .countSubscriptionsBySubscriptionKeyAndPage(subscriptionKey, page.toLong())
                .executeAsOne()
            subscriptionsInDb > 0
        },
        nextKeyProvider = { key, _ -> key + 1 },
        prevKeyProvider = { key, _ -> if (key > 1) key - 1 else null },
        itemsExtractor = { it },
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SelectSubscriptionTopic>,
    ): MediatorResult {
        return delegate.load(loadType, state)
    }
}
