package ai.saniou.thread.data.cache

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Channel
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.db.table.forum.Comment
import app.cash.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightSourceCache(
    db: Database,
) : SourceCache {

    private val topicQueries = db.topicQueries
    private val commentQueries = db.commentQueries
    private val channelQueries = db.channelQueries

    override fun observeTopic(sourceId: String, topicId: String): Flow<Topic?> {
        return topicQueries.getTopic(sourceId, topicId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map {
                it?.let {
                    Topic(
                        it.id,
                        it.sourceId,
                        it.channelId,
                        it.commentCount,
                        it.createdAt,
                        it.userHash,
                        it.authorName,
                        it.title,
                        it.content,
                        it.summary,
                        it.sage,
                        it.admin,
                        it.hide,
                        it.page
                    )
                }
            }
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
        withContext(Dispatchers.IO) {
            topicQueries.upsertTopic(topic)
        }
    }

    override fun saveTopics(topics: List<Topic>) {
        topicQueries.transaction {
            topics.forEach { topic ->
                topicQueries.upsertTopic(topic)
            }
        }
    }

    override fun saveComments(comments: List<Comment>) {
        commentQueries.transaction {
            comments.forEach { reply ->
                commentQueries.upsertComment(reply)
            }
        }
    }

    override suspend fun clearChannelCache(sourceId: String, channelId: String) {
        withContext(Dispatchers.IO) {
            topicQueries.deleteTopicPage(sourceId, channelId)
        }
    }

    override suspend fun clearTopicCommentsCache(sourceId: String, topicId: String) {
        withContext(Dispatchers.IO) {
            commentQueries.deleteCommentsByTopicId(sourceId, topicId)
        }
    }

    override suspend fun updateTopicLastAccessTime(
        sourceId: String,
        topicId: String,
        time: Long,
    ) {
        withContext(Dispatchers.IO) {
            topicQueries.updateTopicLastAccessTime(time, sourceId, topicId)
        }
    }

    override suspend fun updateTopicLastReadCommentId(
        sourceId: String,
        topicId: String,
        commentId: String,
    ) {
        withContext(Dispatchers.IO) {
            topicQueries.updateTopicLastReadCommentId(commentId, sourceId, topicId)
        }
    }

    override suspend fun getChannels(sourceId: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            val allChannels = channelQueries.getChannelsBySource(sourceId).executeAsList()
            sortChannels(allChannels)
        }
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
        withContext(Dispatchers.IO) {
            channelQueries.transaction {
                forums.forEach { forum ->
                    channelQueries.insertChannel(forum)
                }
            }
        }
    }
}
