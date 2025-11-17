package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.nowToEpochMilliseconds
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
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
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.SUBSCRIBE,
                    id = subscriptionKey
                ).executeAsOneOrNull()?.run {
                    // 本地有数据，就不请求网络
                    return MediatorResult.Success(endOfPaginationReached = false)
                }
                1L
            }

            LoadType.PREPEND -> return MediatorResult.Success(true) // 不向前翻

            LoadType.APPEND -> {
                val showId = state.pages.lastOrNull()?.lastOrNull()?.id
                    ?: return MediatorResult.Success(true)
                val showPage = db.subscriptionQueries.getSubscription(subscriptionKey, showId)
                    .executeAsOneOrNull()?.page ?: return MediatorResult.Success(true)

                val remoteKey = db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.SUBSCRIBE,
                    id = subscriptionKey
                ).executeAsOneOrNull() ?: return MediatorResult.Success(true)

                if (remoteKey.currKey > showPage) {
                    return MediatorResult.Success(true)
                }
                remoteKey.nextKey ?: return MediatorResult.Success(true)
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
                        db.threadQueries.insertThread(feed.toTable())

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

                return MediatorResult.Success(endOfPagination)
            }

            is SaniouResponse.Error -> MediatorResult.Error(result.ex)
        }
    }

    private suspend fun feedDetail(
        page: Long,
    ): SaniouResponse<List<Feed>> =
        forumRepository.feed(subscriptionKey, page)
}
