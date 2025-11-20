package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.toFeed
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.SqlDelightPagingSource
import ai.saniou.nmb.data.source.SubscriptionRemoteMediator
import ai.saniou.nmb.db.Database
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class SubscriptionFeedUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    @OptIn(ExperimentalPagingApi::class)
    fun feed(
        subscriptionKey: String,
    ): Flow<PagingData<Feed>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20
            ),
            remoteMediator = SubscriptionRemoteMediator(subscriptionKey, forumRepository, db),
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

}
