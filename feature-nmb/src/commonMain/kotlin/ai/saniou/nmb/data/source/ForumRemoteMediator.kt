package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.data.entity.toTable
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.GetThreadsInForum
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.ExperimentalPagingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalPagingApi::class)
class ForumRemoteMediator(
    private val fid: Long,
    private val fgroup: Long,
    private val forumRepository: ForumRepository,
    private val db: Database,
) : RemoteMediator<Int, GetThreadsInForum>() {

    @OptIn(ExperimentalTime::class)
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GetThreadsInForum>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1L
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                // 查 RemoteKeys 拿 nextPage
                db.remoteKeyQueries.getRemoteKeyById(
                    type = RemoteKeyType.FORUM,
                    id = fid.toString()
                ).executeAsOneOrNull()?.nextKey ?: return MediatorResult.Success(true)
            }
        }


        return when (val result = threadList(page = page)) {
            is SaniouResponse.Success -> {
                val forums = result.data
                val endOfPagination = forums.isEmpty()

                db.transaction {
                    if (loadType == LoadType.REFRESH) {
                        //db.forumQueries.clearForum(fid)
                        //db.remoteKeyQueries.insertKey(forumId = fid, nextPage = null)
                    }

                    forums.forEach { forum ->
                        db.threadQueries.upsetThread(forum.toTable())
                        db.threadQueries.upsertThreadInformation(
                            id = forum.id,
                            remainReplies = forum.remainReplies,
                            lastKey = forum.replies.lastOrNull()?.id ?: forum.id
                        )
                        forum.toTableReply().forEach(db.threadReplyQueries::upsertThreadReply)
                    }
                    db.remoteKeyQueries.insertKey(
                        type = RemoteKeyType.FORUM,
                        id = fid.toString(),
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


    private suspend fun threadList(
        page: Long
    ): SaniouResponse<List<Forum>> =
        if (fgroup == -1L)
            forumRepository.timeline(fid, page)
        else
            forumRepository.showf(fid, page)
}


