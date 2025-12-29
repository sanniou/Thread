package ai.saniou.thread.data.cache

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Channel
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.db.table.forum.Comment
import app.cash.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightSourceCache(
    db: Database
) : SourceCache {

    private val topicQueries = db.topicQueries
    private val commentQueries = db.commentQueries
    private val channelQueries = db.channelQueries

    override fun observeThread(sourceId: String, threadId: String): Flow<Topic?> {
        return topicQueries.getTopic(sourceId, threadId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let { Topic(it.id, it.sourceId, it.channelId, it.commentCount, it.createdAt, it.userHash, it.authorName, it.title, it.content, it.sage, it.admin, it.hide, it.page) } }
    }

    override fun getThreadRepliesPagingSource(
        sourceId: String,
        threadId: String,
        userHash: String?
    ): PagingSource<Int, Comment> {
        return if (userHash != null) {
            QueryPagingSource(
                transacter = commentQueries,
                context = Dispatchers.IO,
                countQuery = commentQueries.countCommentsByTopicIdAndUserHash(
                    sourceId,
                    threadId,
                    userHash
                ),
                queryProvider = { limit, offset ->
                    commentQueries.getCommentsByTopicIdAndUserHashOffset(
                        sourceId,
                        threadId,
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
                countQuery = commentQueries.countCommentsByTopicId(sourceId, threadId),
                queryProvider = { limit, offset ->
                    commentQueries.getCommentsByTopicIdOffset(
                        sourceId,
                        threadId,
                        limit,
                        offset
                    )
                }
            )
        }
    }

    override fun getForumThreadsPagingSource(
        sourceId: String,
        fid: String
    ): PagingSource<Int, GetTopicsInChannelOffset> {
        return QueryPagingSource(
            transacter = topicQueries,
            context = Dispatchers.IO,
            countQuery = topicQueries.countTopicsByChannel(sourceId, fid),
            queryProvider = { limit, offset ->
                topicQueries.getTopicsInChannelOffset(sourceId, fid, limit, offset)
            }
        )
    }

    override suspend fun saveThread(thread: Topic) {
        withContext(Dispatchers.IO) {
            topicQueries.upsertTopic(thread)
        }
    }

    override suspend fun saveThreads(threads: List<Topic>) {
        withContext(Dispatchers.IO) {
            topicQueries.transaction {
                threads.forEach { thread ->
                    topicQueries.upsertTopic(thread)
                }
            }
        }
    }

    override suspend fun saveReplies(replies: List<Comment>) {
        withContext(Dispatchers.IO) {
            commentQueries.transaction {
                replies.forEach { reply ->
                    commentQueries.upsertComment(reply)
                }
            }
        }
    }

    override suspend fun clearForumCache(sourceId: String, fid: String) {
        withContext(Dispatchers.IO) {
            topicQueries.deleteTopicPage(sourceId, fid)
        }
    }

    override suspend fun clearThreadRepliesCache(sourceId: String, threadId: String) {
        withContext(Dispatchers.IO) {
            commentQueries.deleteCommentsByTopicId(sourceId, threadId)
        }
    }

    override suspend fun updateThreadLastAccessTime(sourceId: String, threadId: String, time: Long) {
        withContext(Dispatchers.IO) {
            topicQueries.updateTopicLastAccessTime(time, sourceId, threadId)
        }
    }

    override suspend fun updateTopicLastReadCommentId(sourceId: String, threadId: String, replyId: String) {
        withContext(Dispatchers.IO) {
            topicQueries.updateTopicLastReadCommentId(replyId, sourceId, threadId)
        }
    }

    override suspend fun getForums(sourceId: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            val allChannels = channelQueries.getChannelsBySource(sourceId).executeAsList()
            sortChannels(allChannels)
        }
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

    override suspend fun saveForums(forums: List<Channel>) {
        withContext(Dispatchers.IO) {
            channelQueries.transaction {
                forums.forEach { forum ->
                    channelQueries.insertChannel(forum)
                }
            }
        }
    }
}
