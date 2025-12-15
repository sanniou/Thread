package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.ForumRepository
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import ai.saniou.thread.data.source.discourse.DiscourseSource
import ai.saniou.thread.domain.repository.Source

class ForumRepositoryImpl(
    private val db: Database,
    private val nmbSource: NmbSource,
    private val discourseSource: DiscourseSource,
) : ForumRepository {

    override fun getForumThreadsPaging(
        sourceId: String,
        fid: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        return when (sourceId) {
            "nmb" -> {
                val fidLong = fid.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
                val pagerFlow = if (isTimeline) {
                    nmbSource.getTimelinePager(fidLong, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
                } else {
                    nmbSource.getShowfPager(fidLong, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
                }
                pagerFlow.map { pagingData ->
                    pagingData.map { it.toDomain() }
                }
            }
            "discourse" -> {
                // TODO: Implement paging for discourse properly using Pager
                // For now, we return empty or implement a basic flow if possible,
                // but PagingSource is better.
                // Since DiscourseSource currently returns Result<List<Post>>, we might need to adapt it.
                // However, the task is "add discourse", let's assuming we use PagingSource for it later.
                // For this step, I will throw NotImplemented or return empty to fix compilation first
                // and then we can improve DiscourseSource to support Paging if needed,
                // OR we just wrap the simple list in PagingData for now.
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            }
            else -> kotlinx.coroutines.flow.flowOf(PagingData.empty())
        }
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
