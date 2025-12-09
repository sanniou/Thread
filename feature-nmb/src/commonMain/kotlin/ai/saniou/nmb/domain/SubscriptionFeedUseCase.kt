package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.dto.Feed
import ai.saniou.thread.data.source.nmb.remote.dto.toFeed
import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.SqlDelightPagingSource
import ai.saniou.thread.data.source.nmb.SubscriptionRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


class SubscriptionFeedUseCase(
    private val forumRepository: NmbXdApi,
    private val db: Database,
) {
    @OptIn(ExperimentalPagingApi::class)
    fun feed(
        subscriptionKey: String,
        policy: DataPolicy = DataPolicy.CACHE_FIRST
    ): Flow<PagingData<Feed>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20
            ),
            remoteMediator = if (policy == DataPolicy.API_FIRST) {
                SubscriptionRemoteMediator(subscriptionKey, forumRepository, db)
            } else {
                null
            },
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    countQueryProvider = {
                        db.subscriptionQueries.countSubscriptionsBySubscriptionKey(
                            subscriptionKey
                        )
                    },
                    transacter = db.subscriptionQueries,
                    context = Dispatchers.IO,
                    pageQueryProvider = { page ->
                        db.subscriptionQueries.selectSubscriptionThread(
                            subscriptionKey = subscriptionKey,
                            page = page.toLong()
                        )
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { thread ->
                thread.toFeed()
            }
        }
    }

    suspend fun delFeed(id: String, threadId: Long) = forumRepository.delFeed(id, threadId)

    suspend fun hasLocalSubscriptions(subscriptionKey: String): Boolean = withContext(Dispatchers.IO) {
        db.subscriptionQueries.countLocalSubscriptions(subscriptionKey).executeAsOne() > 0
    }

    suspend fun pushLocalSubscriptions(subscriptionKey: String) = withContext(Dispatchers.IO) {
        val localSubscriptions =
            db.subscriptionQueries.getLocalSubscriptions(subscriptionKey).executeAsList()
        localSubscriptions.forEach {
            forumRepository.addFeed(subscriptionKey, it.threadId)
            db.subscriptionQueries.updateLocalFlag(subscriptionKey, it.threadId)
        }
    }
}
