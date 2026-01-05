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
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult

@OptIn(ExperimentalPagingApi::class)
class SubscriptionRemoteMediator(
    private val subscriptionKey: String,
    private val forumRepository: NmbXdApi,
    private val db: Database,
    private val dataPolicy: DataPolicy,
) : RemoteMediator<Int, SelectSubscriptionTopic>() {

    private val delegate = GenericRemoteMediator<Int, SelectSubscriptionTopic, List<Feed>>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = 1,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = RemoteKeyType.SUBSCRIBE,
            id = subscriptionKey,
        ),
        fetcher = { page -> forumRepository.feed(subscriptionKey, page.toLong()).toResult() },
        saver = { feedDetail, page, loadType ->
            if (loadType == LoadType.REFRESH) {
                db.subscriptionQueries.deleteCloudSubscriptions(subscriptionKey)
            }

            feedDetail.forEach { feed ->
                val topic = feed.toTable("nmb", Long.MAX_VALUE)
                db.topicQueries.upsertTopicNoPage(topic)

                db.subscriptionQueries.insertSubscription(
                    subscriptionKey = subscriptionKey,
                    sourceId = "nmb",
                    topicId = feed.id.toString(),
                    page = page.toLong(),
                    subscriptionTime = feed.nowToEpochMilliseconds(),
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
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() },
        longToKey = { it.toInt() }
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SelectSubscriptionTopic>,
    ): RemoteMediatorMediatorResult {
        return delegate.load(loadType, state)
    }
}
