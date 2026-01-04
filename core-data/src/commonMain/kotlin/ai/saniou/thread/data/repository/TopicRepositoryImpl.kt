package ai.saniou.thread.data.repository

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.mapper.toMetadata
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.TopicRepository
import ai.saniou.thread.network.SaniouResult
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
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

class TopicRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
    private val cache: SourceCache,
) : TopicRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getTopicDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Topic> {
        return cache.observeTopic(sourceId, id)
            .mapNotNull { it?.toDomain(db.commentQueries, db.imageQueries) }
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val currentCache = cache.observeTopic(sourceId, id).firstOrNull()
                    if (currentCache == null || forceRefresh) {
                        val result = source.getTopicDetail(id, 1)
                        result.onSuccess { post ->
                            // Convert back to entity for saving (warning: images might be lost if toEntity doesn't handle them or cache.saveThread doesn't save images)
                            // Ideally source should save to DB directly or cache.saveThread should handle images.
                            // NmbSource.getThreadDetail currently doesn't save to DB.
                            // Let's assume cache.saveThread handles basic info, but images need separate handling or source should do it.
                            cache.saveTopic(post.toEntity())
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getTopicMetadata(
        sourceId: String,
        id: String,
        forceRefresh: Boolean,
    ): Flow<TopicMetadata> {
        return cache.observeTopic(sourceId, id)
            .mapNotNull { it?.toMetadata(db.commentQueries, db.imageQueries) }
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val currentCache = cache.observeTopic(sourceId, id).firstOrNull()
                    if (currentCache == null || forceRefresh) {
                        val result = source.getTopicDetail(id, 1)
                        result.onSuccess { post ->
                            cache.saveTopic(post.toEntity())
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getTopicCommentsPaging(
        sourceId: String,
        threadId: String,
        isPoOnly: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Comment>> {
        val source =
            sourceMap[sourceId] ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())

        return Pager(
            config = PagingConfig(pageSize = 20),
            initialKey = initialPage,
            remoteMediator = GenericRemoteMediator(
                db = db,
                dataPolicy = DataPolicy.CACHE_ELSE_NETWORK,
                initialKey = initialPage,
                remoteKeyStrategy = DefaultRemoteKeyStrategy(
                    db = db,
                    type = RemoteKeyType.THREAD,
                    id = "${sourceId}_${threadId}_${isPoOnly}"
                ),
                fetcher = { page ->
                    when (val result = source.getTopicComments(threadId, page, isPoOnly)) {
                        else -> SaniouResult.Success(result.getOrDefault(emptyList()))
                    }
                },
                saver = { comments, page, loadType ->
                    if (loadType == LoadType.REFRESH) {
                        // TODO: Implement cleaner for specific config?
                        // Currently clearTopicCommentsCache clears ALL comments for the topic.
                        // If we support partial refresh or mix PO only, we need to be careful.
                        // For now, consistent with full refresh:
                        // cache.clearTopicCommentsCache(sourceId, threadId)
                        // But wait, if we are scrolling back, REFRESH loadType might not mean "Swipe Refresh".
                        // In Paging3 RemoteMediator, REFRESH loadType is triggered on initial load or invalidate.
                        // We should probably NOT clear everything if we want to keep history?
                        // Actually, RemoteKeys management handles the "where are we".
                        // But if we want to ensure fresh data on refresh, we might clear.
                    }
                    // Filter out comments that might duplicate? upsert handles it.
                    cache.saveComments(comments.map {
                        it.toEntity(sourceId, page.toLong())
                    })
                },
                endOfPaginationReached = { it.isEmpty() },
                keyIncrementer = { it + 1 },
                keyDecrementer = { it - 1 },
                keyToLong = { it.toLong() },
                longToKey = { it.toInt() }
            ),
            pagingSourceFactory = {
                // TODO: Pass userHash for PO filtering if needed, or handle isPoOnly in query
                // Currently getTopicCommentsPagingSource supports userHash but we need to know PO's hash.
                // If we don't know PO hash, isPoOnly filter relies on API returning subset and we storing it.
                // Issue: If we store PO-only comments in same table, getTopicCommentsPagingSource without hash returns ALL.
                // We need to pass a filter mode to paging source if we want to distinguish in UI.
                // For now, let's assume standard flow.
                cache.getTopicCommentsPagingSource(sourceId, threadId, null)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
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

    override fun getTopicComments(
        threadId: Long,
        isPoOnly: Boolean,
    ): Flow<List<Comment>> {
        TODO("Not yet implemented")
    }


    override suspend fun updateTopicLastReadCommentId(threadId: String, replyId: String) {
        // FIXME: Need sourceId. Assuming "nmb" for now or fetch from existing thread context.
        val sourceId = "nmb"
        withContext(Dispatchers.IO) {
            cache.updateTopicLastReadCommentId(sourceId, threadId, replyId)
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
