package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.DataPolicy
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.GetThreadsInForum
import ai.saniou.nmb.db.table.RemoteKeys
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPagingApi::class)
class ForumRemoteMediator(
    private val sourceId: Long, // 通用源 ID，例如 fid
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val fetcher: suspend (page: Int) -> SaniouResponse<List<Forum>>,
) : RemoteMediator<Int, GetThreadsInForum>() {

    private val threadQueries = db.threadQueries
    private val remoteKeyQueries = db.remoteKeyQueries

    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GetThreadsInForum>,
    ): MediatorResult {
        val page: Long = when (loadType) {
            LoadType.REFRESH -> {
                // 刷新或跳页逻辑
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1) ?: 1
            }

            LoadType.PREPEND -> {
                val remoteKey = getRemoteKeyForFirstItem(state)
                remoteKey?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }

            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                remoteKey?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
            }
        }

        // CACHE_FIRST 策略：在刷新时检查缓存
        if (loadType == LoadType.REFRESH && dataPolicy == DataPolicy.CACHE_FIRST) {
            val threadsInDb = withContext(currentCoroutineContext()) {
                threadQueries.countThreadsByFidAndPage(sourceId, page).asFlow()
                    .mapToOneOrNull(context = coroutineContext).first()
            }
            if (threadsInDb != null && threadsInDb > 0) {
                return MediatorResult.Success(endOfPaginationReached = false)
            }
        }

        return when (val result = fetcher(page.toInt())) {
            is SaniouResponse.Success -> {
                val forums = result.data
                val endOfPagination = forums.isEmpty()

                db.transaction {
                    if (loadType == LoadType.REFRESH) {
                        // API_FIRST 策略：只清理当前页，而不是整个列表
                        threadQueries.deleteThreadsByFidAndPage(sourceId, page.toLong())
                    }

                    val prevKey = if (page == 1L) null else page - 1
                    val nextKey = if (endOfPagination) null else page + 1

                    forums.forEach { forum ->
                        threadQueries.upsetThread(forum.toTable(page.toLong()))
                        threadQueries.upsertThreadInformation(
                            id = forum.id,
                            remainReplies = forum.remainReplies,
                            lastKey = forum.replies.lastOrNull()?.id ?: forum.id
                        )
                        forum.toTableReply().forEach(db.threadReplyQueries::upsertThreadReply)
                    }
                    remoteKeyQueries.insertKey(
                        type = RemoteKeyType.FORUM,
                        id = sourceId.toString(),
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

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, GetThreadsInForum>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { thread ->
                withContext(currentCoroutineContext()) {
                    remoteKeyQueries.getRemoteKeyById(RemoteKeyType.FORUM, thread.fid.toString())
                        .asFlow()
                        .mapToOneOrNull(context = coroutineContext).first()
                }
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, GetThreadsInForum>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { thread ->
                withContext(currentCoroutineContext()) {
                    remoteKeyQueries.getRemoteKeyById(RemoteKeyType.FORUM, thread.fid.toString())
                        .asFlow()
                        .mapToOneOrNull(context = coroutineContext).first()
                }
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, GetThreadsInForum>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.fid?.let { fid ->
                withContext(currentCoroutineContext()) {
                    remoteKeyQueries.getRemoteKeyById(RemoteKeyType.FORUM, fid.toString()).asFlow()
                        .mapToOneOrNull(context = coroutineContext).first()
                }
            }
        }
    }
}
