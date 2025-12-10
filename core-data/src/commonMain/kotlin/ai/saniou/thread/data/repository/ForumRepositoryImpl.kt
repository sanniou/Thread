package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.ForumRepository
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ForumRepositoryImpl(
    private val db: Database,
    private val nmbSource: NmbSource,
) : ForumRepository {

    override fun getForumThreadsPaging(
        fid: Long,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        val pagerFlow = if (isTimeline) {
            nmbSource.getTimelinePager(fid, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
        } else {
            nmbSource.getShowfPager(fid, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
        }
        return pagerFlow.map { pagingData ->
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

    override suspend fun saveLastOpenedForum(forum: Forum?) {
        withContext(Dispatchers.IO) {
            forum?.let {
                db.keyValueQueries.insertKeyValue("last_opened_forum_id", it.id)
                // Also update forum detail in case it's new
                // db.forumQueries.insertForum(it.toTable())
            } ?: db.keyValueQueries.deleteKeyValue("last_opened_forum_id")
        }
    }

    override suspend fun getLastOpenedForum(): Forum? {
        return withContext(Dispatchers.IO) {
            db.keyValueQueries.getKeyValue("last_opened_forum_id").executeAsOneOrNull()?.value_?.let { fid ->
                db.forumQueries.getForum(fid.toLong()).executeAsOneOrNull()?.toDomain()
            }
        }
    }
}
