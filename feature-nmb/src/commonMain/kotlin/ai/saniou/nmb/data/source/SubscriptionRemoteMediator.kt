package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
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
        state: PagingState<Int, ai.saniou.nmb.db.table.Thread>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.SUBSCRIBE,
                    id = subscriptionKey
                )
                    .executeAsOneOrNull()?.run {
                        // 本地有数据，就不请求网络
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                1L
            }

            LoadType.PREPEND -> return MediatorResult.Success(true) // 不向前翻

            LoadType.APPEND -> {
                // 本地还有数据可读？→ 直接 Success，不请求
                val localCount = db.subscriptionQueries
                    .countSubscriptionsBySubscriptionKey(subscriptionKey)
                    .executeAsOne()

                val alreadyRead = state.pages.sumOf { it.data.size }
                if (alreadyRead < localCount)      // 说明本地还有缓存页
                    return MediatorResult.Success(false)

                // 本地掏空 → 查 remoteKeys 决定请求第几页
                db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.SUBSCRIBE,
                    id = subscriptionKey
                ).executeAsOneOrNull()?.nextKey ?: return MediatorResult.Success(true)
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
                            subscriptionTime = Clock.System.now().toEpochMilliseconds()
                        )

                        db.remoteKeyQueries.insertKey(
                            type = RemoteKeyType.SUBSCRIBE,
                            id = subscriptionKey,
                            prevKey = if (page == 1L) null else page - 1,
                            nextKey = if (endOfPagination) null else page + 1,
                            updateAt = Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                }

                return MediatorResult.Success(endOfPagination)
            }

            is SaniouResponse.Error -> MediatorResult.Error(result.ex)
        }
    }

    private suspend fun feedDetail(
        page: Long
    ): SaniouResponse<List<Feed>> =
        forumRepository.feed(subscriptionKey, page)
}
