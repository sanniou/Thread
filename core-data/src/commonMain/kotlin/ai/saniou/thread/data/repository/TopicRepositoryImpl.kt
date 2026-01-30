package ai.saniou.thread.data.repository

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toMetadata
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.tieba.TiebaSource
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.*
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.TopicRepository
import androidx.paging.*
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class TopicRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
    private val cache: SourceCache,
) : TopicRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getTopicDetail(sourceId: String, id: String, forceRefresh: Boolean): Flow<Topic> {
        return cache.observeTopic(sourceId, id)
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val currentCache = db.topicQueries.getTopic(sourceId, id).executeAsOneOrNull()
                    if (currentCache == null || forceRefresh) {
                        val result = source.getTopicDetail(id, 1)
                        result.onSuccess { post ->
                            cache.saveTopic(post)
                        }.onFailure {
                            throw it
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
            .map { it.toMetadata() }
            .onStart {
                val source = sourceMap[sourceId]
                if (source != null) {
                    val currentCache = db.topicQueries.getTopic(sourceId, id).executeAsOneOrNull()
                    if (currentCache == null || forceRefresh) {
                        val result = source.getTopicDetail(id, 1)
                        result.onSuccess { post ->
                            cache.saveTopic(post)
                        }.onFailure {
                            throw it
                        }
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getTopicCommentsPager(
        sourceId: String,
        topicId: String,
        isPoOnly: Boolean,
    ): Flow<PagingData<Comment>> {
        val source = sourceMap[sourceId]
            ?: return flowOf(PagingData.empty())

        return Pager(
            config = PagingConfig(pageSize = 30),
            remoteMediator = GenericRemoteMediator(
                db = db,
                dataPolicy = DataPolicy.CACHE_ELSE_NETWORK,
                remoteKeyStrategy = DefaultRemoteKeyStrategy(
                    db = db,
                    type = "thread_${sourceId}_${topicId}_${if (isPoOnly) "po" else "all"}",
                    itemTargetIdExtractor = { comment -> comment.id }
                ),
                fetcher = { cursor ->
                    source.getTopicComments(topicId, cursor, isPoOnly)
                },
                saver = { comments, loadType, receiveDate, startOrder ->
                    cache.saveComments(comments, sourceId, receiveDate, startOrder)
                },
                itemTargetIdExtractor = { comment -> comment.id },
                cacheChecker = { cursor ->
                    // 使用 RemoteKeys 检查指定 cursor 的数据是否已缓存
                    // cursor 对应 prevKey 或 nextKey
                    val remoteKeyType = "thread_${sourceId}_${topicId}_${if (isPoOnly) "po" else "all"}"
                    db.remoteKeyQueries.hasRemoteKeyWithCursor(
                        type = remoteKeyType,
                        prevKey = cursor,
                        nextKey = cursor
                    ).executeAsOne()
                },
                lastItemMetadataExtractor = { topic ->
                    // 用不到所以先随便写
                    topic.floor to topic.floor
                }
            ),
            pagingSourceFactory = {
                if (isPoOnly) {
                    QueryPagingSource(
                        transacter = db,
                        context = Dispatchers.IO,
                        countQuery =
                            db.commentQueries.countCommentsByTopicIdPoMode(
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
                } else {
                    QueryPagingSource(
                        transacter = db,
                        context = Dispatchers.IO,
                        countQuery =
                            db.commentQueries.countCommentsByTopicId(
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
        ).flow.map { it.map { it.toDomain(db.imageQueries) } }
    }

    override suspend fun getSubComments(
        sourceId: String,
        topicId: String,
        commentId: String,
        page: Int,
    ): Result<List<Comment>> {
        val source = sourceMap[sourceId]
            ?: return Result.failure(Exception("Source not found: $sourceId"))

        if (source !is TiebaSource) {
            return Result.failure(Exception("getSubComments is not supported for source: $sourceId"))
        }
        return source.getSubComments(topicId, commentId, page)
    }

    override fun getTopicImages(sourceId: String, threadId: Long): Flow<List<Image>> {
        return db.imageQueries.getImagesByParent(
            sourceId = sourceId,
            parentId = threadId.toString(),
            parentType = ImageType.Comment
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
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
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { it.toDomain(db.imageQueries) }
            }
    }


    override suspend fun updateTopicLastReadCommentId(
        sourceId: String,
        threadId: String,
        replyId: String,
    ) {
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
