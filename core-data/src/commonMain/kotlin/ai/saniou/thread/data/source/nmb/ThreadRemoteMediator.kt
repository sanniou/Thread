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
    private val fetcher: suspend (page: Int) -> SaniouResponse<Thread>,
) : RemoteMediator<Int, ThreadReply>() {

    private val delegate = GenericRemoteMediator<Int, ThreadReply, Thread>(
        db = db,
        dataPolicy = dataPolicy,
        initialKey = initialPage,
        remoteKeyStrategy = DefaultRemoteKeyStrategy(
            db = db,
            type = RemoteKeyType.THREAD,
            id = threadId,
            itemIdExtractor = { it.threadId.toString() }
        ),
        fetcher = { page -> fetcher(page) },
        saver = { threadDetail, page ->
            db.threadQueries.upsertThread(threadDetail.toTable(sourceId, page.toLong()))
            threadDetail.toTableReply(sourceId, page.toLong())
                .filter { it.userHash != "Tips" } // 过滤广告
                .forEach(db.threadReplyQueries::upsertThreadReply)
        },
        itemsExtractor = { threadDetail ->
            // 过滤广告后判断是否为空
            threadDetail.toTableReply(sourceId, 0L)
                .filter { it.userHash != "Tips" }
        },
        cacheChecker = { page ->
            val repliesInDb = db.threadReplyQueries.countRepliesByThreadIdAndPage(sourceId, threadId, page.toLong())
                .executeAsOne()
            repliesInDb > 0
        },
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() }
    )

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ThreadReply>
    ): RemoteMediatorMediatorResult {
        return delegate.load(loadType, state)
    }
}