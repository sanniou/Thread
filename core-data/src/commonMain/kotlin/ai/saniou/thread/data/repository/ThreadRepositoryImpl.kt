package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.ThreadRepository
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class ThreadRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
) : ThreadRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    private val nmbSource by lazy {
        sourceMap["nmb"] as? NmbSource ?: throw IllegalStateException("NmbSource not found")
    }

    override fun getThreadDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Post> {
        return if (sourceId == "nmb") {
            val longId = id.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf()
            db.threadQueries.getThread(longId)
                .asFlow()
                .mapToOneOrNull(Dispatchers.IO)
                .mapNotNull { it?.toDomain() }
                .onStart {
                    val needsFetch =
                        forceRefresh || db.threadQueries.getThread(longId)
                            .executeAsOneOrNull() == null
                    if (needsFetch) {
                        nmbSource.getThreadRepliesByPage(longId, 1)
                    }
                }
                .flowOn(Dispatchers.IO)
        } else {
            flow {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val result = source.getThreadDetail(id, 1)
                    result.getOrNull()?.let { emit(it) }
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    override fun getThreadRepliesPaging(
        sourceId: String,
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>> {
        return if (sourceId == "nmb") {
            val longId =
                threadId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
            val poUserHash = if (isPoOnly) {
                db.threadQueries.getThread(longId).executeAsOneOrNull()?.userHash
            } else {
                null
            }
            nmbSource.getThreadRepliesPager(
                threadId = longId,
                poUserHash = poUserHash,
                policy = DataPolicy.CACHE_ELSE_NETWORK,
                initialPage = initialPage
            )
        } else {
            val source = sourceMap[sourceId]
            source?.getThreadRepliesPager(threadId, initialPage)
                ?: kotlinx.coroutines.flow.flowOf(PagingData.empty())
        }
    }

    override fun getThreadImages(threadId: Long): Flow<List<Image>> =
        db.threadReplyQueries.getThreadImages(threadId)
            .asFlow()
            .map { query ->
                query.executeAsList().map {
                    Image(
                        name = it.img,
                        ext = it.ext
                    )
                }
            }
            .flowOn(Dispatchers.IO)

    override fun getThreadReplies(
        threadId: Long,
        isPoOnly: Boolean,
    ): Flow<List<ThreadReply>> {
        val poUserHash = if (isPoOnly) {
            db.threadQueries.getThread(threadId).executeAsOneOrNull()?.userHash
        } else {
            null
        }
        TODO()
        return db.threadReplyQueries.getThreadReplies(threadId, limit = 1000, offset = 0)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        withContext(Dispatchers.IO) {
            db.threadQueries.updateThreadLastAccessTime(time, threadId)
        }
    }

    override suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long) {
        withContext(Dispatchers.IO) {
            db.threadQueries.updateThreadLastReadReplyId(replyId, threadId)
        }
    }
}
