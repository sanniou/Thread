package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.data.entity.toForumWithReply
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

class ForumUserCase(
    private val forumRepository: ForumRepository,
    private val db: Database

) {
    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        id: Long,
        fgroup: Long
    ): Flow<PagingData<Forum>> {
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 3),
            remoteMediator = ForumRemoteMediator(id, fgroup, forumRepository, db),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.forumQueries.countForum(),
                    transacter = db.forumQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        db.forumQueries.queryForums(fid = id, limit = limit, offset = offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { forum ->
                forum.toForumWithReply(
                    db.forumQueries.queryReply(forum.id, 10L, 0L).executeAsList()
                )
            }
        }
    }
}
