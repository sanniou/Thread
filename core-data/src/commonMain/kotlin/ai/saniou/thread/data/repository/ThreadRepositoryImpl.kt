package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.repository.ThreadRepository
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class ThreadRepositoryImpl(
    private val db: Database,
    private val nmbSource: NmbSource,
) : ThreadRepository {

    override fun getThreadDetail(id: Long, forceRefresh: Boolean): Flow<Post> =
        db.threadQueries.getThread(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapNotNull { it?.toDomain() }
            .onStart {
                val needsFetch =
                    forceRefresh || db.threadQueries.getThread(id).executeAsOneOrNull() == null
                if (needsFetch) {
                    nmbSource.getThreadRepliesByPage(id, 1)
                }
            }
            .flowOn(Dispatchers.IO)

    override fun getThreadRepliesPaging(
        threadId: Long,
        isPoOnly: Boolean,
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>> {
        val poUserHash = if (isPoOnly) {
            db.threadQueries.getThread(threadId).executeAsOneOrNull()?.userHash
        } else {
            null
        }
        return nmbSource.getThreadRepliesPager(
            threadId = threadId,
            poUserHash = poUserHash,
            policy = DataPolicy.CACHE_ELSE_NETWORK,
            initialPage = initialPage
        ).map { pagingData ->
            pagingData.map { it.toDomain() }
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
