package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.ThreadReply
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator
import app.cash.paging.RemoteMediatorMediatorResult

@OptIn(ExperimentalPagingApi::class)
class ThreadRemoteMediator(
    private val sourceId: String,
    private val threadId: String,
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialPage: Int,
    private val isPoOnly: Boolean = false,
    private val fetcher: suspend (page: Int) -> SaniouResponse<Thread>,
) : RemoteMediator<Int, ThreadReply>() {

    // When isPoOnly is true, we store pages as negative numbers to distinguish from normal pages
    // page 1 -> -1
    // page 2 -> -2
    // ...
    // This allows storing both "all replies" and "po only replies" in the same table without conflict
    // for the same threadId.
    private fun Int.toStoragePage(): Long {
        return if (isPoOnly) -this.toLong() else this.toLong()
    }

    private val delegate = GenericRemoteMediator<Int, ThreadReply, Thread>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = initialPage,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = if (isPoOnly) RemoteKeyType.THREAD_PO else RemoteKeyType.THREAD,
            id = threadId,
            itemIdExtractor = { it.threadId.toString() }
        ),
        fetcher = { page -> fetcher(page) },
        saver = { threadDetail, page, _ ->
            val storagePage = page.toStoragePage()
            // Use upsertThreadNoPage to avoid overwriting the 'page' field (which represents Forum Page)
            // with the Reply Page number, unless it's a new insertion.
            db.threadQueries.upsertThreadNoPage(threadDetail.toTable(sourceId, storagePage))
            threadDetail.toTableReply(sourceId, storagePage)
                .filter { it.userHash != "Tips" } // 过滤广告
                .forEach(db.threadReplyQueries::upsertThreadReply)
        },
        endOfPaginationReached = { threadDetail ->
            // 过滤广告后判断是否为空
            val storagePage = 0L // Doesn't matter for check, using 0L as dummy
            threadDetail.toTableReply(sourceId, storagePage)
                .filter { it.userHash != "Tips" }
                .isEmpty()
        },
        cacheChecker = { page ->
            val storagePage = page.toStoragePage()
            val repliesInDb = db.threadReplyQueries.countRepliesByThreadIdAndPage(
                sourceId,
                threadId,
                storagePage
            )
                .executeAsOne()
            repliesInDb > 0
        },
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() },
        longToKey = { it.toInt() }
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ThreadReply>
    ): RemoteMediatorMediatorResult {
        return delegate.load(loadType, state)
    }
}