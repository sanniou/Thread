package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetThreadsInForumOffset
import ai.saniou.thread.db.table.RemoteKeys
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.network.SaniouResponse
import ai.saniou.thread.data.paging.DataPolicy
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
class ForumRemoteMediator(
    private val sourceId: String,
    private val fid: String,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int,
    private val fetcher: suspend (page: Int) -> SaniouResponse<List<Forum>>,
) : RemoteMediator<Int, GetThreadsInForumOffset>() {

    private val threadQueries = db.threadQueries
    private val remoteKeyQueries = db.remoteKeyQueries

    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GetThreadsInForumOffset>,
    ): RemoteMediatorMediatorResult {
        val page: Long = when (loadType) {
            LoadType.REFRESH -> {
                // 刷新或跳页逻辑
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1) ?: initialPage.toLong()
            }

            LoadType.PREPEND -> {
                val remoteKey = getRemoteKeyForFirstItem(state)
                remoteKey?.prevKey
                    ?: return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = remoteKey != null)
            }

            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                remoteKey?.nextKey
                    ?: return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = remoteKey != null)
            }
        }

        // 根据策略决定是否发起网络请求
        when (dataPolicy) {
            DataPolicy.NETWORK_ELSE_CACHE -> {
                return when (val result = fetcher(page.toInt())) {
                    is SaniouResponse.Success -> {
                        val forums = result.data
                        val endOfPagination = forums.isEmpty()

                        db.transaction {
                            if (loadType == LoadType.REFRESH) {
                                threadQueries.deleteThreadsByFidAndPage(sourceId, fid, page)
                            }

                            val prevKey = if (page == 1L) null else page - 1
                            val nextKey = if (endOfPagination) null else page + 1

                            forums.forEach { forum ->
                                threadQueries.upsertThread(forum.toTable(sourceId, page.toLong()))
                                threadQueries.upsertThreadInformation(
                                    id = forum.id.toString(),
                                    sourceId = sourceId,
                                    remainReplies = forum.remainReplies,
                                    lastKey = forum.replies.lastOrNull()?.id ?: forum.id
                                )
                                forum.toTableReply(sourceId)
                                    .forEach(db.threadReplyQueries::upsertThreadReply)
                            }
                            remoteKeyQueries.insertKey(
                                type = RemoteKeyType.FORUM,
                                id = fid,
                                prevKey = prevKey?.toLong(),
                                currKey = page.toLong(),
                                nextKey = nextKey?.toLong(),
                                updateAt = Clock.System.now().toEpochMilliseconds(),
                            )
                        }
                        RemoteMediatorMediatorResultSuccess(endOfPaginationReached = endOfPagination)
                    }

                    is SaniouResponse.Error -> {
                        // 网络失败时，依赖本地缓存，所以返回成功让 PagingSource 加载
                        RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)
                    }
                }
            }

            DataPolicy.CACHE_ELSE_NETWORK -> {
                val threadsInDb =
                    threadQueries.countThreadsByFidAndPage(sourceId, fid, page).executeAsOne()
                if (threadsInDb > 0) {
                    return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = false)
                }
                // 缓存不存在，流程同上
                return when (val result = fetcher(page.toInt())) {
                    is SaniouResponse.Success -> {
                        val forums = result.data
                        val endOfPagination = forums.isEmpty()

                        db.transaction {
                            val prevKey = if (page == 1L) null else page - 1
                            val nextKey = if (endOfPagination) null else page + 1

                            forums.forEach { forum ->
                                threadQueries.upsertThread(forum.toTable(sourceId, page.toLong()))
                                threadQueries.upsertThreadInformation(
                                    id = forum.id.toString(),
                                    sourceId = sourceId,
                                    remainReplies = forum.remainReplies,
                                    lastKey = forum.replies.lastOrNull()?.id ?: forum.id
                                )
                                forum.toTableReply(sourceId)
                                    .forEach(db.threadReplyQueries::upsertThreadReply)
                            }
                            remoteKeyQueries.insertKey(
                                type = RemoteKeyType.FORUM,
                                id = fid,
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

            else -> return RemoteMediatorMediatorResultSuccess(endOfPaginationReached = true)  // CACHE_ONLY or NETWORK_ONLY
        }
    }

    private fun getRemoteKeyForLastItem(state: PagingState<Int, GetThreadsInForumOffset>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { thread ->
                remoteKeyQueries.getRemoteKeyById(RemoteKeyType.FORUM, thread.fid.toString())
                    .executeAsOneOrNull()
            }
    }

    private fun getRemoteKeyForFirstItem(state: PagingState<Int, GetThreadsInForumOffset>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { thread ->
                remoteKeyQueries.getRemoteKeyById(RemoteKeyType.FORUM, thread.fid.toString())
                    .executeAsOneOrNull()
            }
    }

    private fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, GetThreadsInForumOffset>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.fid?.let { fid ->
                remoteKeyQueries.getRemoteKeyById(RemoteKeyType.FORUM, fid.toString())
                    .executeAsOneOrNull()
            }
        }
    }
}
