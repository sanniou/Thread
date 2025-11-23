package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.nowToEpochMilliseconds
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
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
    private val forumRepository: ForumRepository,
    private val db: Database,
) : RemoteMediator<Int, ai.saniou.nmb.db.table.Thread>() {


    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.Thread>,
    ): RemoteMediatorMediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.SUBSCRIBE,
                    id = subscriptionKey
                ).executeAsOneOrNull()?.run {
                    // 本地有数据，就不请求网络
                    return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = false)
                }
                1L
            }

            LoadType.PREPEND -> return RemoteMediatorMediatorResultSuccess(true) // 不向前翻

            LoadType.APPEND -> {
                val showId = state.pages.lastOrNull()?.lastOrNull()?.id
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
                        //db.forumQueries.clearForum(fid)
                        //db.remoteKeyQueries.insertKey(forumId = fid, nextPage = null)
                    }

                    feedDetail.forEach { feed ->
                        db.threadQueries.upsetThread(feed.toTable(page))

                        db.subscriptionQueries.insertSubscription(
                            subscriptionKey = subscriptionKey, threadId = feed.id,
                            page = page,
                            subscriptionTime =  feed.nowToEpochMilliseconds(),
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
