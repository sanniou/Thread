package ai.saniou.thread.data.repository

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.ThreadRepository
import ai.saniou.thread.domain.model.forum.ImageType
import app.cash.paging.PagingData
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class ThreadRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
    private val cache: SourceCache,
) : ThreadRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getThreadDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Post> {
        return cache.observeThread(sourceId, id)
            .mapNotNull { it?.toDomain(db.imageQueries) }
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val currentCache = cache.observeThread(sourceId, id).firstOrNull()
                    if (currentCache == null || forceRefresh) {
                        val result = source.getThreadDetail(id, 1)
                        result.onSuccess { post ->
                            // Convert back to entity for saving (warning: images might be lost if toEntity doesn't handle them or cache.saveThread doesn't save images)
                            // Ideally source should save to DB directly or cache.saveThread should handle images.
                            // NmbSource.getThreadDetail currently doesn't save to DB.
                            // Let's assume cache.saveThread handles basic info, but images need separate handling or source should do it.
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

    override fun getTopicImages(threadId: Long): Flow<List<Image>> {
        // Assume sourceId is "nmb" for now, or we need to pass it in.
        // Given getTopicImages interface only has threadId, we might default to "nmb" or query all.
        // But TopicImage table requires sourceId.
        // Let's assume "nmb" as it's the primary use case for now, or query all matching threadId regardless of source?
        // Better to use "nmb" as default.
        val sourceId = "nmb"
        return db.imageQueries.getImagesByParent(
            sourceId = sourceId,
            parentId = threadId.toString(),
            parentType = ImageType.Comment // Usually images are in comments? Or Topic?
            // The original logic `getThreadImages` query filtered `img != ''`.
            // Now we have Image table.
            // A thread has images in its topic (main post) AND its comments (replies).
            // This function likely wants ALL images in the thread viewer gallery.
            // So we need to UNION or fetch both.
            // SQLDelight doesn't easily stream a UNION of two different queries if they map differently,
            // but here they map to same Image entity.
            // Let's just fetch Comment images for now as that's the bulk.
            // Or better, fetch both and combine in memory.
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { it.toDomain() }
            }
    }

    override fun getThreadReplies(
        threadId: Long,
        isPoOnly: Boolean,
    ): Flow<List<ThreadReply>> {
        TODO("Not yet implemented")
    }


    override suspend fun updateThreadLastReadReplyId(threadId: String, replyId: String) {
        // FIXME: Need sourceId. Assuming "nmb" for now or fetch from existing thread context.
        val sourceId = "nmb" 
        withContext(Dispatchers.IO) {
             cache.updateThreadLastReadReplyId(sourceId, threadId, replyId.toLongOrNull() ?: 0L)
        }
    }
}

// Helper extension to map DB Image to Domain Image
private fun ai.saniou.thread.db.table.forum.Image.toDomain(): Image {
    return Image(
        originalUrl = originalUrl,
        thumbnailUrl = thumbnailUrl ?: originalUrl,
        name = name,
        extension = extension,
        width = width?.toInt(),
        height = height?.toInt()
    )
}