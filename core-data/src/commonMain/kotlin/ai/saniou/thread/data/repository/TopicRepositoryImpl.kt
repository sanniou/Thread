package ai.saniou.thread.data.repository

import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.mapper.toMetadata
import ai.saniou.thread.data.model.CommentKey
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.paging.KeysetPagingSource
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.tieba.TiebaSource
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.TopicRepository
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
                initialKey = 1,
                remoteKeyStrategy = DefaultRemoteKeyStrategy(
                    db = db,
                    type = RemoteKeyType.THREAD,
                    id = "${sourceId}_${topicId}_${if (isPoOnly) "po" else "all"}",
                    serializer = { it.toString() },
                    deserializer = { it.toInt() }
                ),
                fetcher = { page ->
                    source.getTopicComments(topicId, page, isPoOnly)
                },
                saver = { comments, page, _ ->
                    cache.saveComments(comments, sourceId, page)
                },
                itemsExtractor = { it },
                endOfPaginationReached = { comments ->
                    comments.isEmpty()
                },
                cacheChecker = { page ->
                    if (isPoOnly) {
                        db.commentQueries.countCommentsByTopicIdPoModeAndPage(
                            sourceId,
                            topicId,
                            page.toLong()
                        ).executeAsOne() > 0
                    } else {
                        db.commentQueries.countCommentsByTopicIdAndPage(
                            sourceId,
                            topicId,
                            page.toLong()
                        ).executeAsOne() > 0
                    }
                },
                keyExtractor = { comment ->
                    comment.floor.toInt()
                }
            ),
            pagingSourceFactory = {
                // Use CommentKey (floor, id) for DB paging
                val initialKey = CommentKey(0, "")
                if (isPoOnly) {
                    KeysetPagingSource(
                        transacter = db,
                        context = Dispatchers.IO,
                        countQueryProvider = {
                            db.commentQueries.countCommentsByTopicIdPoMode(
                                sourceId,
                                topicId
                            )
                        },
                        queryProvider = { key, limit ->
                            val currentKey = key ?: initialKey
                            db.commentQueries.getCommentsPoModeKeyset(
                                sourceId,
                                topicId,
                                currentKey.floor,
                                currentKey.id,
                                limit
                            )
                        },
                        keyExtractor = { comment ->
                            CommentKey(comment.floor ?: 0, comment.id)
                        }
                    )
                } else {
                    KeysetPagingSource(
                        transacter = db,
                        context = Dispatchers.IO,
                        countQueryProvider = {
                            db.commentQueries.countCommentsByTopicId(
                                sourceId,
                                topicId
                            )
                        },
                        queryProvider = { key, limit ->
                            val currentKey = key ?: initialKey
                            db.commentQueries.getCommentsKeyset(
                                sourceId,
                                topicId,
                                currentKey.floor,
                                currentKey.id,
                                limit
                            )
                        },
                        keyExtractor = { comment ->
                            CommentKey(comment.floor ?: 0, comment.id)
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
