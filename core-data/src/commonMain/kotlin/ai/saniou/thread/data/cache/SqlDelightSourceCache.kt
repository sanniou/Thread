package ai.saniou.thread.data.cache

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.TopicTag
import ai.saniou.thread.db.table.forum.Channel
import ai.saniou.thread.db.table.forum.Comment
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import app.cash.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import ai.saniou.thread.domain.model.forum.Comment as DomainComment

class SqlDelightSourceCache(
    val db: Database,
) : SourceCache {

    private val topicQueries = db.topicQueries
    private val commentQueries = db.commentQueries
    private val channelQueries = db.channelQueries
    private val topicTagQueries = db.topicTagQueries

    override fun observeTopic(sourceId: String, topicId: String): Flow<Topic> {
        return topicQueries.getTopic(sourceId, topicId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .filterNotNull()
            .map { it.toDomain(commentQueries, db.imageQueries, topicTagQueries) }
    }

    override fun getTopicCommentsPagingSource(
        sourceId: String,
        topicId: String,
        userHash: String?,
    ): PagingSource<Int, Comment> {
        return if (userHash != null) {
            QueryPagingSource(
                transacter = commentQueries,
                context = Dispatchers.IO,
                countQuery = commentQueries.countCommentsByTopicIdAndUserHash(
                    sourceId,
                    topicId,
                    userHash
                ),
                queryProvider = { limit, offset ->
                    commentQueries.getCommentsByTopicIdAndUserHashOffset(
                        sourceId,
                        topicId,
                        userHash,
                        limit,
                        offset
                    )
                }
            )
        } else {
            QueryPagingSource(
                transacter = commentQueries,
                context = Dispatchers.IO,
                countQuery = commentQueries.countCommentsByTopicId(sourceId, topicId),
                queryProvider = { limit, offset ->
                    commentQueries.getCommentsByTopicIdOffset(
                        sourceId,
                        topicId,
                        limit,
                        offset
                    )
                }
            )
        }
    }

    override fun getChannelTopicPagingSource(
        sourceId: String,
        channelId: String,
    ): PagingSource<Int, GetTopicsInChannelOffset> {
        return QueryPagingSource(
            transacter = topicQueries,
            context = Dispatchers.IO,
            countQuery = topicQueries.countTopicsByChannel(sourceId, channelId),
            queryProvider = { limit, offset ->
                topicQueries.getTopicsInChannelOffset(sourceId, channelId, limit, offset)
            }
        )
    }

    override suspend fun saveTopic(topic: Topic) {
        saveTopics(listOf(topic), false, topic.sourceName, topic.channelId)
    }

    override suspend fun saveTopics(
        topics: List<Topic>,
        clearPage: Boolean,
        sourceId: String,
        channelId: String,
        page: Int?,
    ) {
        topicQueries.transaction {
            // 1. 清理旧数据: 将数据库中 page >= 当前page 的数据全部 +1 ，用作于缓存
            if (clearPage && page != null) {
                topicQueries.incrementTopicPage(sourceId, channelId, page.toLong())
            }

            // 2. 保存新数据
            topics.forEach { topic ->
                val topicPage = page ?: 1
                val topicEntity = topic.toEntity(page = topicPage)
                topicQueries.upsertTopic(topicEntity)

                // Save Tags
                topic.tags.forEach { tag ->
                    db.tagQueries.insert(tag.toEntity())
                    topicTagQueries.insert(
                        TopicTag(
                            sourceId = topic.sourceId,
                            topicId = topic.id,
                            tagId = tag.id
                        )
                    )
                }

                if (topic.remainingCount != null) {
                    topicQueries.upsertTopicInformation(
                        id = topic.id,
                        sourceId = topic.sourceName,
                        remainingCount = topic.remainingCount,
                        lastReplyAt = topic.orderKey ?: 0L
                    )
                }

                // Save Images
                topic.images.forEachIndexed { index, image ->
                    db.imageQueries.upsertImage(
                        image.toEntity(
                            sourceId = sourceId,
                            parentId = topic.id,
                            parentType = ImageType.Topic,
                            sortOrder = index.toLong()
                        )
                    )
                }

                // Save Preview Comments
                topic.comments.forEach { comment ->
                    commentQueries.upsertComment(
                        comment.toEntity(sourceId, Long.MIN_VALUE)
                    )
                    // Save Comment Images
                    comment.images.forEachIndexed { index, image ->
                        db.imageQueries.upsertImage(
                            image.toEntity(
                                sourceId = sourceId,
                                parentId = comment.id,
                                parentType = ImageType.Comment,
                                sortOrder = index.toLong()
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun saveComments(comments: List<DomainComment>, sourceId: String, page: Int) {
        commentQueries.transaction {
            comments.forEach { comment ->
                commentQueries.upsertComment(
                    comment.toEntity(sourceId, page.toLong())
                )
                // Save Comment Images
                comment.images.forEachIndexed { index, image ->
                    db.imageQueries.upsertImage(
                        image.toEntity(
                            sourceId = sourceId,
                            parentId = comment.id,
                            parentType = ImageType.Comment,
                            sortOrder = index.toLong()
                        )
                    )
                }
            }
        }
    }

    override suspend fun clearChannelCache(sourceId: String, channelId: String) {
        topicQueries.deleteTopicPage(sourceId, channelId)
    }

    override suspend fun clearTopicCommentsCache(sourceId: String, topicId: String) {
        commentQueries.deleteCommentsByTopicId(sourceId, topicId)
    }

    override suspend fun updateTopicLastAccessTime(
        sourceId: String,
        topicId: String,
        time: Long,
    ) {
        topicQueries.updateTopicLastAccessTime(time, sourceId, topicId)
    }

    override suspend fun updateTopicLastReadCommentId(
        sourceId: String,
        topicId: String,
        commentId: String,
    ) {
        topicQueries.updateTopicLastReadCommentId(commentId, sourceId, topicId)
    }

    override fun getChannels(sourceId: String): List<Channel> {
        val allChannels = channelQueries.getChannelsBySource(sourceId).executeAsList()
        return sortChannels(allChannels)
    }

    override fun observeChannels(sourceId: String): Flow<List<Channel>> {
        return channelQueries.getChannelsBySource(sourceId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sortChannels(it) }
    }

    private fun sortChannels(channels: List<Channel>): List<Channel> {
        // Group by parentId to build a tree
        val childrenMap = channels.filter { it.parentId != null }
            .groupBy { it.parentId }

        // Find roots (channels with no parent or parent not in list)
        // Note: Some channels might have a parentId that doesn't exist in the current list
        // (e.g. if we only synced a subset), treat them as roots if parent is missing.
        val allIds = channels.map { it.id }.toSet()
        val roots = channels.filter { it.parentId == null || it.parentId !in allIds }
            .sortedBy { it.sort } // Assuming 'sort' column is populated, else maybe sort by name?

        val result = mutableListOf<Channel>()

        fun traverse(channel: Channel) {
            result.add(channel)
            childrenMap[channel.id]?.sortedBy { it.sort }?.forEach { child ->
                traverse(child)
            }
        }

        roots.forEach { root ->
            traverse(root)
        }

        return result
    }

    override suspend fun saveChannels(forums: List<Channel>) {
        channelQueries.transaction {
            forums.forEach { forum ->
                channelQueries.insertChannel(forum)
            }
        }
    }
}
