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
import kotlinx.coroutines.flow.firstOrNull
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

    override fun getForumName(sourceId: String, fid: String): Flow<String?> {
        val source = sourceMap[sourceId] ?: return kotlinx.coroutines.flow.flowOf(null)
        return source.getForum(fid).map { it?.name }
    }

    override fun getForumDetail(sourceId: String, fid: String): Flow<Forum?> {
        val source = sourceMap[sourceId] ?: return kotlinx.coroutines.flow.flowOf(null)
        return source.getForum(fid)
    }

    override suspend fun saveLastOpenedForum(forum: Forum?) {
        withContext(Dispatchers.IO) {
            if (forum != null) {
                db.keyValueQueries.insertKeyValue("last_opened_forum_id", forum.id)
                db.keyValueQueries.insertKeyValue("last_opened_forum_source", forum.sourceName)
            } else {
                db.keyValueQueries.deleteKeyValue("last_opened_forum_id")
                db.keyValueQueries.deleteKeyValue("last_opened_forum_source")
            }
        }
    }

    override suspend fun getLastOpenedForum(): Forum? {
        return withContext(Dispatchers.IO) {
            val fid = db.keyValueQueries.getKeyValue("last_opened_forum_id").executeAsOneOrNull()?.content
            val sourceId =
                db.keyValueQueries.getKeyValue("last_opened_forum_source").executeAsOneOrNull()?.content
                    ?: "nmb"

            if (fid != null) {
                // 如果是 NMB，尝试从数据库获取完整信息
                // 如果是其他源，可能需要通过 source.getForum 获取，或者仅仅返回一个只有 ID 的 Forum 对象
                // 这里为了简单，如果数据库有就从数据库取（适用于 NMB），否则构造一个简单的对象
                val source = sourceMap[sourceId]
                // 尝试从 Source 获取
                if (source != null) {
                    try {
                        source.getForum(fid).firstOrNull()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
}
