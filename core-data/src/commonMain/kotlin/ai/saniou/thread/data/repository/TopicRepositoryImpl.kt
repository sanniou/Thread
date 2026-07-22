package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.cache.CacheFreshnessStore
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toMetadata
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.*
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.TopicRepository
import ai.saniou.thread.domain.source.SourceCatalog
import ai.saniou.thread.domain.cache.CachePolicyProvider
import ai.saniou.thread.domain.cache.CacheResource
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import androidx.paging.*
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class TopicRepositoryImpl(
    private val db: Database,
    private val sourceCatalog: SourceCatalog,
    private val cache: SourceCache,
    private val refreshCoordinator: RefreshCoordinator,
    private val cachePolicyProvider: CachePolicyProvider,
    private val freshnessStore: CacheFreshnessStore,
) : TopicRepository {

    override fun getTopicDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Topic> {
        return cache.observeTopic(sourceId, id)
            .map { topic ->
                topic.copy(sourceUrl = sourceCatalog.source(sourceId)?.topicUrl(id).orEmpty())
            }
            .onStart {
                val source = sourceCatalog.source(sourceId)
                if (source != null) {
                    val currentCache = db.topicQueries.getTopic(sourceId, id).executeAsOneOrNull()
                    val policy = cachePolicyProvider.policy(sourceId, CacheResource.TOPIC_DETAIL)
                    val key = CacheFreshnessStore.topic(sourceId, id)
                    if (currentCache == null || forceRefresh || !freshnessStore.isFresh(key, policy)) {
                        val result = refreshCoordinator.execute(
                            key = "forum:$sourceId:topic:$id",
                            label = "${source.name} 主题",
                            operation = { source.getTopicDetail(id, 1) },
                        )
                        result.onSuccess { post ->
                            cache.saveTopic(post)
                            freshnessStore.markFresh(key)
                        }.onFailure {
                            if (currentCache == null || !policy.serveStaleOnFailure) throw it
                        }
                    }
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun getTopicMetadata(
        sourceId: String,
        id: String,
        forceRefresh: Boolean,
    ): Flow<TopicMetadata> {
        return cache.observeTopic(sourceId, id)
            .map { topic ->
                val source = sourceCatalog.source(sourceId)
                topic.toMetadata(
                    capabilities = source?.capabilities ?: ai.saniou.thread.domain.model.SourceCapabilities.Default,
                    sourceUrl = source?.topicUrl(id).orEmpty(),
                )
            }
            .onStart {
                val source = sourceCatalog.source(sourceId)
                if (source != null) {
                    val currentCache = db.topicQueries.getTopic(sourceId, id).executeAsOneOrNull()
                    val policy = cachePolicyProvider.policy(sourceId, CacheResource.TOPIC_DETAIL)
                    val key = CacheFreshnessStore.topic(sourceId, id)
                    if (currentCache == null || forceRefresh || !freshnessStore.isFresh(key, policy)) {
                        val result = refreshCoordinator.execute(
                            key = "forum:$sourceId:topic:$id",
                            label = "${source.name} 主题",
                            operation = { source.getTopicDetail(id, 1) },
                        )
                        result.onSuccess { post ->
                            cache.saveTopic(post)
                            freshnessStore.markFresh(key)
                        }.onFailure {
                            if (currentCache == null || !policy.serveStaleOnFailure) throw it
                        }
                    }
                }
            }
            .flowOn(ioDispatcher)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getTopicCommentsPager(
        sourceId: String,
        topicId: String,
        isPoOnly: Boolean,
        isReverse: Boolean,
        startPage: Int,
    ): Flow<PagingData<Comment>> {
        val source = sourceCatalog.source(sourceId)
            ?: return flowOf(PagingData.empty())

        val viewMode = buildString {
            append(if (isPoOnly) "po" else "all")
            if (isReverse) append("_desc")
        }
        val refreshCursor = startPage.takeIf { it > 1 }?.toString()

        return Pager(
            config = threadPagingConfig(pageSize = 30),
            remoteMediator = GenericRemoteMediator(
                db = db,
                dataPolicy = DataPolicy.CACHE_ELSE_NETWORK,
                remoteKeyStrategy = DefaultRemoteKeyStrategy(
                    db = db,
                    type = "thread_${sourceId}_${topicId}_${viewMode}_p${startPage.coerceAtLeast(1)}",
                    itemTargetIdExtractor = { comment -> comment.id }
                ),
                fetcher = { cursor ->
                    source.getTopicComments(
                        threadId = topicId,
                        cursor = cursor,
                        isPoOnly = isPoOnly,
                        isReverse = isReverse,
                    )
                },
                saver = { comments, loadType, cursor, receiveDate, startOrder ->
                    val page = cursor?.toLongOrNull() ?: startPage.toLong().coerceAtLeast(1L)
                    if (loadType == LoadType.REFRESH) {
                        cache.clearTopicCommentsCache(sourceId, topicId)
                    }
                    cache.saveComments(
                        comments = comments,
                        sourceId = sourceId,
                        topicId = topicId,
                        viewMode = viewMode,
                        page = page,
                        receiveDate = receiveDate,
                        startOrder = startOrder
                    )
                },
                itemTargetIdExtractor = { comment -> comment.id },
                cacheChecker = { cursor ->
                    val page = cursor?.toIntOrNull() ?: startPage.coerceAtLeast(1)
                    db.commentQueries.hasCommentListingForPage(
                        sourceId = sourceId,
                        topicId = topicId,
                        viewMode = viewMode,
                        page = page.toLong()
                    ).executeAsOne()
                },
                initializeAction = {
                    // 跳页 / 倒序切换必须强制拉网，避免错页缓存
                    if (startPage > 1 || isReverse) {
                        return@GenericRemoteMediator RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
                    }
                    val hasCachedComments = db.commentQueries.countCommentsByTopicId(sourceId, topicId)
                        .executeAsOne() > 0L
                    val policy = cachePolicyProvider.policy(sourceId, CacheResource.TOPIC_COMMENTS)
                    val isFresh = freshnessStore.isFresh(
                        CacheFreshnessStore.comments(sourceId, topicId),
                        policy,
                    )
                    if (hasCachedComments && isFresh) {
                        RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
                    } else {
                        RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
                    }
                },
                onRefreshSuccess = {
                    freshnessStore.markFresh(CacheFreshnessStore.comments(sourceId, topicId))
                },
                lastItemMetadataExtractor = { comment ->
                    comment.floor to comment.floor
                },
                refreshCursor = refreshCursor,
            ),
            pagingSourceFactory = {
                when {
                    isPoOnly && isReverse -> QueryPagingSource(
                        transacter = db,
                        context = ioDispatcher,
                        countQuery = db.commentQueries.countCommentsByTopicIdPoMode(
                            sourceId = sourceId,
                            topicId = topicId
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsPoModeKeysetDesc(
                                sourceId = sourceId,
                                topicId = topicId,
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                    isPoOnly -> QueryPagingSource(
                        transacter = db,
                        context = ioDispatcher,
                        countQuery = db.commentQueries.countCommentsByTopicIdPoMode(
                            sourceId = sourceId,
                            topicId = topicId
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsPoModeKeyset(
                                sourceId = sourceId,
                                topicId = topicId,
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                    isReverse -> QueryPagingSource(
                        transacter = db,
                        context = ioDispatcher,
                        countQuery = db.commentQueries.countCommentsByTopicId(
                            sourceId,
                            topicId
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsKeysetDesc(
                                sourceId = sourceId,
                                topicId = topicId,
                                offset = offset,
                                limit = limit
                            )
                        }
                    )
                    else -> QueryPagingSource(
                        transacter = db,
                        context = ioDispatcher,
                        countQuery = db.commentQueries.countCommentsByTopicId(
                            sourceId,
                            topicId
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsKeyset(
                                sourceId = sourceId,
                                topicId = topicId,
                                offset = offset,
                                limit = limit
                            )
                        }
                    )
                }
            }
        ).flow.map { it.map { it.toDomain(db.imageQueries, db.commentQueries) } }
    }

    override suspend fun fetchTopicImagePage(
        sourceId: String,
        threadId: String,
        channelId: String,
        channelName: String,
        picId: String,
        picIndex: String,
        seeLz: Boolean,
        forward: Boolean,
        batchSize: Int,
    ): Result<List<Image>> {
        val source = sourceCatalog.source(sourceId)
            ?: return Result.failure(IllegalArgumentException("Unknown source: $sourceId"))
        return source.fetchTopicImagePage(
            threadId = threadId,
            channelId = channelId,
            channelName = channelName,
            picId = picId,
            picIndex = picIndex,
            seeLz = seeLz,
            forward = forward,
            batchSize = batchSize,
        )
    }

    override suspend fun getSubComments(
        sourceId: String,
        topicId: String,
        commentId: String,
        page: Int,
    ): Result<List<Comment>> {
        val connector = sourceCatalog.subComments(sourceId)
            ?: return Result.failure(UnsupportedOperationException("Source '$sourceId' has no sub-comments"))
        return connector.getSubComments(topicId, commentId, page)
    }

    override fun getTopicImages(sourceId: String, threadId: String): Flow<List<Image>> {
        return db.imageQueries.getImagesByParent(
            sourceId = sourceId,
            parentId = threadId,
            parentType = ImageType.Comment
        )
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map { it.toDomain() }
            }
    }

    override fun getTopicComments(
        sourceId: String,
        topicId: String,
        isPoOnly: Boolean,
    ): Flow<List<Comment>> {
        val query = if (isPoOnly) {
            db.commentQueries.getAllCommentsByTopicIdPoMode(sourceId, topicId)
        } else {
            db.commentQueries.getAllCommentsByTopicId(sourceId, topicId)
        }

        return query
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map { it.toDomain(db.imageQueries, db.commentQueries) }
            }
    }


    override suspend fun updateTopicLastReadCommentId(
        sourceId: String,
        threadId: String,
        replyId: String,
    ) {
        withContext(ioDispatcher) {
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
