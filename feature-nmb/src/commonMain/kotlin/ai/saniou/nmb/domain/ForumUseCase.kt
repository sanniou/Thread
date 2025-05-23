package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.toForumThreadWithReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.ForumRemoteMediator
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.map
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ForumUseCase(
    private val forumRepository: ForumRepository,
    private val db: Database,
) {
    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        fid: Long,
        fgroup: Long
    ): Flow<PagingData<Forum>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                initialLoadSize = 20 * 2,
                maxSize = 20 * 5,
            ),
            remoteMediator = ForumRemoteMediator(fid, fgroup, forumRepository, db),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.threadQueries.countThread(fid),
                    transacter = db.threadQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        db.threadQueries.queryThreadsInForum(
                            fid = fid,
                            limit = limit,
                            offset = offset
                        )
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { thread ->
                thread.toForumThreadWithReply(
                    db.threadReplyQueries.getLastFiveReplies(thread.id)
                        .executeAsList().sortedBy { it.id }
                )
            }
        }
    }

    fun getForumName(fid: Long): String =
        db.forumQueries.getForum(fid).executeAsOneOrNull()?.name ?: "未知版面"
}
