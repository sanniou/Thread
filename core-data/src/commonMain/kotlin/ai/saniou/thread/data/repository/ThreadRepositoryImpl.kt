package ai.saniou.thread.data.repository

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.forum.ThreadReply
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.ThreadRepository
import app.cash.paging.PagingData
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart

class ThreadRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
    private val cache: SourceCache,
) : ThreadRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getThreadDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Post> {
        return cache.observeThread(sourceId, id)
            .mapNotNull { it?.toDomain() }
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val currentCache = cache.observeThread(sourceId, id).firstOrNull()
                    if (currentCache == null || forceRefresh) {
                        val result = source.getThreadDetail(id, 1)
                        result.onSuccess { post ->
                            cache.saveThread(post.toEntity())
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getThreadRepliesPaging(
        sourceId: String,
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int,
    ): Flow<PagingData<ThreadReply>> {
        val source = sourceMap[sourceId]
        return source?.getThreadRepliesPager(threadId, initialPage, isPoOnly)
            ?: kotlinx.coroutines.flow.flowOf(PagingData.empty())
    }

    override fun getThreadImages(threadId: Long): Flow<List<Image>> =
        db.threadReplyQueries.getThreadImages(
            "nmb",
            threadId.toString()
        ) // FIXME: sourceId hardcoded or need parameter
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
        TODO("Not yet implemented")
    }


    override suspend fun updateThreadLastReadReplyId(threadId: String, replyId: String) {
        // FIXME: Need sourceId
        // withContext(Dispatchers.IO) {
        //     cache.updateThreadLastReadReplyId(sourceId, threadId.toString(), replyId)
        // }
    }
}
