package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.ForumDetail
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.ForumRepository
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ForumRepositoryImpl(
    private val db: Database,
    private val api: NmbXdApi,
) : ForumRepository {

    override fun getForumThreadsPaging(
        fid: Long,
        isTimeline: Boolean,
        initialPage: Int
    ): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            initialKey = initialPage,
            pagingSourceFactory = {
                if (isTimeline) {
                    TimelinePagingSource(api, db, fid)
                } else {
                    ForumPagingSource(api, db, fid)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getForumName(fid: Long): Flow<String> =
        db.forumQueries.getForum(fid)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it.name }
            .flowOn(Dispatchers.IO)

    override fun getForumDetail(fid: Long): Flow<Forum?> =
        db.forumQueries.getForum(fid)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
}
