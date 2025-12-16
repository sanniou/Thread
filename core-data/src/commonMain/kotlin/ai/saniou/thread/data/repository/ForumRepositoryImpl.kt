package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.ForumRepository
import app.cash.paging.PagingData
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import ai.saniou.thread.domain.repository.Source

class ForumRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
) : ForumRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getForumThreadsPaging(
        sourceId: String,
        fid: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        val source = sourceMap[sourceId]
        return source?.getThreadsPager(fid, isTimeline, initialPage)
            ?: kotlinx.coroutines.flow.flowOf(PagingData.empty())
    }

    override fun getForumName(fid: Long): Flow<String?> =
        db.forumQueries.getForum(fid)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.name }
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
