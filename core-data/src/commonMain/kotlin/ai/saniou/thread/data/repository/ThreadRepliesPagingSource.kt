package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultError
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import toTableThreadReply

class ThreadRepliesPagingSource(
    private val api: NmbXdApi,
    private val db: Database,
    private val threadId: Long,
    private val poUserHash: String?
) : PagingSource<Int, ThreadReply>() {

    override fun getRefreshKey(state: PagingState<Int, ThreadReply>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, ThreadReply> {
        val page = params.key ?: 1
        val localData = db.threadReplyQueries
            .getThreadRepliesByPage(threadId, poUserHash, page.toLong())
            .executeAsList()

        if (localData.isNotEmpty()) {
            return PagingSourceLoadResultPage(
                data = localData,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (localData.isEmpty()) null else page + 1
            )
        }

        return when (val response = api.thread(threadId, page.toLong())) {
            is SaniouResponse.Success -> {
                val replies = response.data.replies
                db.transaction {
                    response.data.toTableThreadReply(page)
                        .forEach(db.threadReplyQueries::upsertThreadReply)
                }
                PagingSourceLoadResultPage(
                    data = response.data.toTableThreadReply(page),
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (replies.isEmpty()) null else page + 1
                )
            }

            is SaniouResponse.Error -> {
                PagingSourceLoadResultError<Int, ThreadReply>(response.ex)
            }
        }
    }
}
