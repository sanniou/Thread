package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPagingApi::class)
class ThreadRemoteMediator(
    private val threadId: Long,
    private val po: Boolean,
    private val forumRepository: ForumRepository,
    private val db: Database,
    private val initialPage: Int? = null
) : RemoteMediator<Int, ai.saniou.nmb.db.table.ThreadReply>() {


    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                if (initialPage != null) {
                    initialPage.toLong()
                } else {
                    db.remoteKeyQueries.getRemoteKeyById(
                        type = RemoteKeyType.THREAD,
                        id = threadId.toString()
                    ).executeAsOneOrNull()?.run {
                        // 本地有数据，就不请求网络
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    1L
                }
            }

            LoadType.PREPEND -> return MediatorResult.Success(true) // 不向前翻

            LoadType.APPEND -> {

                val showPage = state.pages.last().last().page

                val remoteKey = db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.THREAD,
                    id = threadId.toString()
                ).executeAsOneOrNull() ?: return MediatorResult.Success(true)

                if (remoteKey.currKey > showPage) {
                    return MediatorResult.Success(true)
                }
                remoteKey.nextKey ?: return MediatorResult.Success(true)
            }
        }



        return when (val result = threadDetail(page = page)) {
            is SaniouResponse.Success -> {
                val threadDetail = result.data
                // 9999999 是系统 Tips ,所以等于空数据就是最后一页
                val endOfPagination = threadDetail.replies.none { it.id != 9999999L }

                db.transaction {
                    if (loadType == LoadType.REFRESH) {
                        //db.forumQueries.clearForum(fid)
                        //db.remoteKeyQueries.insertKey(forumId = fid, nextPage = null)
                    }

                    db.threadQueries.insertThread(threadDetail.toTable())
                    threadDetail.toTableReply(page)
                        .forEach(db.threadReplyQueries::insertThreadReply)

                    db.remoteKeyQueries.insertKey(
                        type = RemoteKeyType.THREAD,
                        id = threadId.toString(),
                        prevKey = if (page == 1L) null else page - 1,
                        currKey = page,
                        nextKey = if (endOfPagination) null else page + 1,
                        updateAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }

                return MediatorResult.Success(endOfPagination)
            }

            is SaniouResponse.Error -> MediatorResult.Error(result.ex)
        }
    }

    private suspend fun threadDetail(
        page: Long
    ): SaniouResponse<Thread> =
        if (po) forumRepository.po(threadId, page) else forumRepository.thread(threadId, page)
}
