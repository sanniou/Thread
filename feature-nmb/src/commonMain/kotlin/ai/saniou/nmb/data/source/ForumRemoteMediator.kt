package ai.saniou.nmb.data.source

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.toTableForum
import ai.saniou.nmb.data.entity.toTableReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.db.Database
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.PagingState
import app.cash.paging.RemoteMediator

@OptIn(ExperimentalPagingApi::class)
class ForumRemoteMediator(
    private val fid: Long,
    private val fgroup: Long,
    private val forumRepository: ForumRepository,
    private val db: Database,
) : RemoteMediator<Int, ai.saniou.nmb.db.table.Forum>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ai.saniou.nmb.db.table.Forum>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1L
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val last = state.lastItemOrNull() ?: return MediatorResult.Success(true)
                (last.id / state.config.pageSize + 1)
            }
        }


        return when (val result = threadList(page = page)) {
            is SaniouResponse.Success -> {
                val forumList = result.data
                if (loadType == LoadType.REFRESH) {
                    // 可选
                    // db.forumQueries.deleteForumPage(fid)
                    // db.forumQueries.deleteReplyPage(fid)
                }
                forumList.forEach {
                    db.forumQueries.insertForum(it.toTableForum())
                    it.toTableReply().forEach { reply ->
                        db.forumQueries.insertReply(reply)
                    }
                }

                MediatorResult.Success(forumList.isEmpty())
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
