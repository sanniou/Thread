package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.Thread
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultError
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState

class ForumPagingSource(
    private val api: NmbXdApi,
    private val db: Database,
    private val fid: Long
) : PagingSource<Int, Thread>() {

    override fun getRefreshKey(state: PagingState<Int, Thread>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, Thread> {
        val page = params.key ?: 1
        val localData = db.threadQueries.getThreadsByForum(fid, page.toLong()).executeAsList()

        if (localData.isNotEmpty()) {
            return PagingSourceLoadResultPage(
                data = localData,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (localData.isEmpty()) null else page + 1
            )
        }

        return when (val response = api.showf(fid, page.toLong())) {
            is SaniouResponse.Success -> {
                val threads = response.data
                db.transaction {
                    threads.forEach {
                        db.threadQueries.upsertThread(it.toTable(page.toLong()))
                    }
                }
                PagingSourceLoadResultPage(
                    data = threads.map { it.toTable(page = page.toLong()) },
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (threads.isEmpty()) null else page + 1
                )
            }

            is SaniouResponse.Error -> {
                PagingSourceLoadResultError<Int, Thread>(response.ex)
            }
        }
    }
}
