package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.DataPolicy
import ai.saniou.nmb.db.Database
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
    private val threadId: Long,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int,
    private val fetcher: suspend (page: Int) -> SaniouResponse<Thread>,
) : RemoteMediator<Int, ai.saniou.nmb.db.table.ThreadReply>() {

    private val threadQueries = db.threadQueries
    private val threadReplyQueries = db.threadReplyQueries
    private val remoteKeyQueries = db.remoteKeyQueries

    override suspend fun initialize(): RemoteMediatorInitializeAction {
        return super.initialize()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>,
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

        if (dataPolicy == DataPolicy.CACHE_FIRST) {
            val repliesInDb =
                threadReplyQueries.countRepliesByThreadIdAndPage(threadId, page.toLong())
                    .executeAsOne()

            if (repliesInDb > 0) {
                return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = false)
            }
        }

        return when (val result = fetcher(page)) {
            is SaniouResponse.Success -> {
                val threadDetail = result.data
                // 非 Tips 回复数
                var validRreplySize = threadDetail.replies.filter { it.userHash != "Tips" }.size
                val endOfPagination =
                    // 有效回复数小于等于数据库中的回复数
                    threadDetail.replyCount <= threadReplyQueries.countValidThreadReplies(threadId)
                        .executeAsOne() + validRreplySize
                            // 或者没有非 Tips 回复
                            || validRreplySize == 0

                db.transaction {
//                    // 刷新时，只清理当前页
//                    if (loadType == LoadType.REFRESH) {
//                        threadReplyQueries.deleteRepliesByThreadIdAndPage(threadId, page.toLong())
//                    }

                    // 更新主楼信息和回复
                    threadQueries.upsetThread(threadDetail.toTable(page.toLong()))
                    threadDetail.toTableReply(page.toLong())
                        .forEach(threadReplyQueries::upsertThreadReply)

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
                RemoteMediatorMediatorResultSuccess(endOfPaginationReached = endOfPagination)
            }

            is SaniouResponse.Error -> RemoteMediatorMediatorResultError(result.ex)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>): ai.saniou.nmb.db.table.RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { reply ->
                remoteKeyQueries.getRemoteKeyById(
                    RemoteKeyType.THREAD,
                    reply.threadId.toString()
                ).executeAsOneOrNull()
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>): ai.saniou.nmb.db.table.RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { reply ->
                remoteKeyQueries.getRemoteKeyById(
                    RemoteKeyType.THREAD,
                    reply.threadId.toString()
                ).executeAsOneOrNull()
            }
    }

    private fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>): ai.saniou.nmb.db.table.RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.threadId?.let { tid ->
                remoteKeyQueries.getRemoteKeyById(RemoteKeyType.THREAD, tid.toString())
                    .executeAsOneOrNull()
            }
        }
    }
}
