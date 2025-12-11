package ai.saniou.thread.data.source.nmb

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.Feed
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.nowToEpochMilliseconds
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult
import app.cash.paging.RemoteMediatorMediatorResultError
import app.cash.paging.RemoteMediatorMediatorResultSuccess
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPagingApi::class)
class SubscriptionRemoteMediator(
    private val subscriptionKey: String,
    private val forumRepository: NmbXdApi,
    private val db: Database,
) : RemoteMediator<Int, ai.saniou.nmb.db.table.SelectSubscriptionThread>() {


    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.SelectSubscriptionThread>,
    ): RemoteMediatorMediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1L

            LoadType.PREPEND -> return RemoteMediatorMediatorResultSuccess(true)   // 不向前翻

            LoadType.APPEND -> {
                val showId = state.pages.lastOrNull()?.data?.lastOrNull()?.id
                    ?: return RemoteMediatorMediatorResultSuccess(true)
                val showPage = db.subscriptionQueries.getSubscription(subscriptionKey, showId)
                    .executeAsOneOrNull()?.page ?: return RemoteMediatorMediatorResultSuccess(true)

                val remoteKey = db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.SUBSCRIBE,
                    id = subscriptionKey
                ).executeAsOneOrNull() ?: return RemoteMediatorMediatorResultSuccess(true)

                if (remoteKey.currKey > showPage) {
                    return RemoteMediatorMediatorResultSuccess(true)
                }
                remoteKey.nextKey ?: return RemoteMediatorMediatorResultSuccess(true)
            }
        }



        return when (val result = feedDetail(page = page)) {
            is SaniouResponse.Success -> {
                val feedDetail = result.data
                val endOfPagination = feedDetail.isEmpty()

                db.transaction {
                    if (loadType == LoadType.REFRESH) {
                        db.subscriptionQueries.deleteCloudSubscriptions(subscriptionKey)
                    }

                    feedDetail.forEach { feed ->
                        db.threadQueries.upsertThreadNoPage(feed.toTable(Long.MAX_VALUE))

                        db.subscriptionQueries.insertSubscription(
                            subscriptionKey = subscriptionKey, threadId = feed.id,
                            page = page,
                            subscriptionTime = feed.nowToEpochMilliseconds(),
                            isLocal = 0
                        )
                    }
                    db.remoteKeyQueries.insertKey(
                        type = RemoteKeyType.SUBSCRIBE,
                        id = subscriptionKey,
                        prevKey = if (page == 1L) null else page - 1,
                        currKey = page,
                        nextKey = if (endOfPagination) null else page + 1,
                        updateAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }

                return RemoteMediatorMediatorResultSuccess(endOfPagination)
            }

            is SaniouResponse.Error -> RemoteMediatorMediatorResultError(result.ex)
        }
    }

    private suspend fun feedDetail(
        page: Long,
    ): SaniouResponse<List<Feed>> =
        forumRepository.feed(subscriptionKey, page)
}
