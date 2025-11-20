package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.DataPolicy
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPagingApi::class)
class ThreadRemoteMediator(
    private val threadId: Long,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val fetcher: suspend (page: Int) -> SaniouResponse<Thread>
) : RemoteMediator<Int, ai.saniou.nmb.db.table.ThreadReply>() {

    private val threadQueries = db.threadQueries
    private val threadReplyQueries = db.threadReplyQueries
    private val remoteKeyQueries = db.remoteKeyQueries

    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>
    ): MediatorResult {
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1)?.toInt() ?: 1
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true) // 不向前翻页
            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                remoteKey?.nextKey?.toInt() ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }
        }

        if (loadType == LoadType.REFRESH && dataPolicy == DataPolicy.CACHE_FIRST) {
            val repliesInDb = withContext(coroutineContext) {
                threadReplyQueries.countRepliesByThreadIdAndPage(threadId, page.toLong()).asFlow().mapToOneOrNull(
                    currentCoroutineContext()
                ).first()
            }
            if (repliesInDb != null && repliesInDb > 0) {
                return MediatorResult.Success(endOfPaginationReached = false)
            }
        }

        return when (val result = fetcher(page)) {
            is SaniouResponse.Success -> {
                val threadDetail = result.data
                val endOfPagination = threadDetail.replies.isEmpty()

                db.transaction {
                    // 刷新时，只清理当前页
                    if (loadType == LoadType.REFRESH) {
                        threadReplyQueries.deleteRepliesByThreadIdAndPage(threadId, page.toLong())
                    }

                    // 更新主楼信息和回复
                    threadQueries.upsetThread(threadDetail.toTable(page.toLong()))
                    threadDetail.toTableReply(page.toLong()).forEach(threadReplyQueries::upsertThreadReply)

                    val prevKey = if (page == 1) null else page - 1
                    val nextKey = if (endOfPagination) null else page + 1

                    remoteKeyQueries.insertKey(
                        type = RemoteKeyType.THREAD,
                        id = threadId.toString(),
                        prevKey = prevKey?.toLong(),
                        currKey = page.toLong(),
                        nextKey = nextKey?.toLong(),
                        updateAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
                MediatorResult.Success(endOfPaginationReached = endOfPagination)
            }
            is SaniouResponse.Error -> MediatorResult.Error(result.ex)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>): ai.saniou.nmb.db.table.RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { reply ->
                withContext(currentCoroutineContext()) {
                    remoteKeyQueries.getRemoteKeyById(RemoteKeyType.THREAD, reply.threadId.toString()).asFlow().mapToOneOrNull(currentCoroutineContext()).first()
                }
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>): ai.saniou.nmb.db.table.RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.threadId?.let { tid ->
                withContext(currentCoroutineContext()) {
                    remoteKeyQueries.getRemoteKeyById(RemoteKeyType.THREAD, tid.toString()).asFlow().mapToOneOrNull(currentCoroutineContext()).first()
                }
            }
        }
    }
}
