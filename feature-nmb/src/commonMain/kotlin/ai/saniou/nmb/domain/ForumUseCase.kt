package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.toForumThreadWithReply
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.source.ForumRemoteMediator
import ai.saniou.nmb.data.source.SqlDelightPagingSource
import ai.saniou.nmb.db.Database
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
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
                SqlDelightPagingSource(
                    transacter = db.threadQueries,
                    context = Dispatchers.IO,
                    countQueryProvider = { db.threadQueries.countThread(fid) },
                    queryProvider = { limit, offset ->
                        db.threadQueries.getThreadsInForum(
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
