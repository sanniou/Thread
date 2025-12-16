package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.network.SaniouResponse
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.db.Database
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorInitializeAction
import app.cash.paging.RemoteMediatorMediatorResult
import app.cash.paging.RemoteMediatorMediatorResultError
import app.cash.paging.RemoteMediatorMediatorResultSuccess
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPagingApi::class)
class ThreadRemoteMediator(
    private val sourceId: String,
    private val threadId: String,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int,
    private val fetcher: suspend (page: Int) -> SaniouResponse<Thread>,
) : RemoteMediator<Int, ai.saniou.thread.db.table.forum.ThreadReply>() {

    private val threadQueries = db.threadQueries
    private val threadReplyQueries = db.threadReplyQueries
    private val remoteKeyQueries = db.remoteKeyQueries

    override suspend fun initialize(): RemoteMediatorInitializeAction {
        return super.initialize()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.thread.db.table.forum.ThreadReply>,
    ): RemoteMediatorMediatorResult {
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1)?.toInt() ?: initialPage
            }

            LoadType.PREPEND -> {
                val remoteKey = getRemoteKeyForFirstItem(state)
                remoteKey?.prevKey?.toInt()
                    ?: return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)
            }

            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                remoteKey?.nextKey?.toInt()
                    ?: return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)
            }
        }

        when (dataPolicy) {
            DataPolicy.CACHE_ELSE_NETWORK -> {
                val repliesInDb =
                    threadReplyQueries.countRepliesByThreadIdAndPage(sourceId, threadId, page.toLong())
                        .executeAsOne()
                if (repliesInDb > 0) {
                    return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = false)
                }
                // 缓存不存在，则继续执行网络请求
            }

            DataPolicy.NETWORK_ELSE_CACHE -> {
                // 直接执行网络请求
            }

            else -> return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)
        }


        return when (val result = fetcher(page)) {
            is SaniouResponse.Success -> {
                val threadDetail = result.data
                val validReplySize = threadDetail.replies.count { it.userHash != "Tips" }
                val endOfPagination =
                    threadDetail.replyCount <= threadReplyQueries.countValidThreadReplies(sourceId, threadId)
                        .executeAsOne() + validReplySize || validReplySize == 0

                db.transaction {
                    threadQueries.upsertThread(threadDetail.toTable(sourceId, page.toLong()))
                    threadDetail.toTableReply(sourceId, page.toLong())
                        .forEach(threadReplyQueries::upsertThreadReply)

                    val prevKey = if (page == 1) null else page - 1
                    val nextKey = if (endOfPagination) null else page + 1

                    remoteKeyQueries.insertKey(
                        type = RemoteKeyType.THREAD,
                        id = threadId,
                        prevKey = prevKey?.toLong(),
                        currKey = page.toLong(),
                        nextKey = nextKey?.toLong(),
                        updateAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
                RemoteMediatorMediatorResultSuccess(endOfPaginationReached = endOfPagination)
            }

            is SaniouResponse.Error -> {
                if (dataPolicy == DataPolicy.NETWORK_ELSE_CACHE) {
                    // 网络失败时，依赖本地缓存，返回成功
                    RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)
                } else {
                    RemoteMediatorMediatorResultError(result.ex)
                }
            }
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ai.saniou.thread.db.table.forum.ThreadReply>): ai.saniou.thread.db.table.RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { reply ->
                remoteKeyQueries.getRemoteKeyById(
                    RemoteKeyType.THREAD,
                    reply.threadId.toString()
                ).executeAsOneOrNull()
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, ai.saniou.thread.db.table.forum.ThreadReply>): ai.saniou.thread.db.table.RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { reply ->
                remoteKeyQueries.getRemoteKeyById(
                    RemoteKeyType.THREAD,
                    reply.threadId.toString()
                ).executeAsOneOrNull()
            }
    }

    private fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, ai.saniou.thread.db.table.forum.ThreadReply>): ai.saniou.thread.db.table.RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.threadId?.let { tid ->
                remoteKeyQueries.getRemoteKeyById(RemoteKeyType.THREAD, tid.toString())
                    .executeAsOneOrNull()
            }
        }
    }
}
