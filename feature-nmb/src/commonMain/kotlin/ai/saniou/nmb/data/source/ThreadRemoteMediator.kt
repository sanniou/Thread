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
    private val forumRepository: ForumRepository,
    private val db: Database,
    private val initialPage: Int? = null,
) : RemoteMediator<Int, ai.saniou.nmb.db.table.ThreadReply>() {


    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.ThreadReply>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                if (initialPage != null) {
                    val isPageExist =
                        db.threadReplyQueries.isPageExist(threadId, initialPage.toLong())
                            .executeAsOne()
                    if (isPageExist) {
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    initialPage.toLong()
                } else {
                    val remoteKey = db.remoteKeyQueries.getRemoteKeyById(
                        type = RemoteKeyType.THREAD,
                        id = threadId.toString()
                    ).executeAsOneOrNull()
                    remoteKey?.currKey ?: 1L
                }
            }

            LoadType.PREPEND -> return MediatorResult.Success(true) // 不向前翻

            LoadType.APPEND -> {
                val remoteKey = db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.THREAD,
                    id = threadId.toString()
                ).executeAsOneOrNull()
                remoteKey?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return when (val result = threadDetail(page = page)) {
            is SaniouResponse.Success -> {
                val threadDetail = result.data
                val endOfPagination =
                    if (threadDetail.replyCount == db.threadReplyQueries.countThreadReplies(threadId)
                            .executeAsOne()
                    ) {
                        true
                    } else {
                        threadDetail.replies.count { it.id != 9999999L } == 0
                    }

                db.transaction {
                    db.threadQueries.upsetThread(threadDetail.toTable())
                    threadDetail.toTableReply(page)
                        .forEach(db.threadReplyQueries::upsertThreadReply)

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
        page: Long,
    ): SaniouResponse<Thread> =
        forumRepository.thread(threadId, page)
}
