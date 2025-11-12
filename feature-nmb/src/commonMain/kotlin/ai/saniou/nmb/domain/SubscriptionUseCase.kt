package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.toFeed
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.SubscriptionRemoteMediator
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.map
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class SubscriptionUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    @OptIn(ExperimentalPagingApi::class)
    fun feed(
        subscriptionKey: String
    ): Flow<PagingData<Feed>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                initialLoadSize = 20 * 2,
                maxSize = 20 * 5,
            ),
            remoteMediator = SubscriptionRemoteMediator(subscriptionKey, forumRepository, db),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.subscriptionQueries.countSubscriptionsBySubscriptionKey(
                        subscriptionKey
                    ),
                    transacter = db.subscriptionQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        db.subscriptionQueries.selectSubscriptionThread(
                            subscriptionKey = subscriptionKey,
                            limit = limit,
                            offset = offset
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
