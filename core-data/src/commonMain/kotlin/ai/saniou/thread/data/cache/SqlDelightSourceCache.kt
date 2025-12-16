package ai.saniou.thread.data.cache

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Thread
import ai.saniou.thread.db.table.forum.ThreadReply
import app.cash.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SqlDelightSourceCache(
    private val db: Database
) : SourceCache {

    private val threadQueries = db.threadQueries
    private val threadReplyQueries = db.threadReplyQueries

    override fun observeThread(sourceId: String, threadId: String): Flow<Thread?> {
        return threadQueries.getThread(sourceId, threadId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    override fun getThreadRepliesPagingSource(
        sourceId: String,
        threadId: String,
        userHash: String?
    ): PagingSource<Int, ThreadReply> {
        return if (userHash != null) {
            QueryPagingSource(
                transacter = threadReplyQueries,
                context = Dispatchers.IO,
                countQuery = threadReplyQueries.countRepliesByThreadIdAndUserHash(
                    sourceId,
                    threadId,
                    userHash
                ),
                queryProvider = { limit, offset ->
                    threadReplyQueries.getRepliesByThreadIdAndUserHashOffset(
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
                transacter = threadReplyQueries,
                context = Dispatchers.IO,
                countQuery = threadReplyQueries.countRepliesByThreadId(sourceId, threadId),
                queryProvider = { limit, offset ->
                    threadReplyQueries.getRepliesByThreadIdOffset(
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
    ): PagingSource<Int, Thread> {
        return QueryPagingSource(
            transacter = threadQueries,
            context = Dispatchers.IO,
            countQuery = threadQueries.countThreadsByFid(sourceId, fid),
            queryProvider = { limit, offset ->
                threadQueries.getThreadsInForumOffset(sourceId, fid, limit, offset)
            }
        )
    }

    override suspend fun saveThread(thread: Thread) {
        withContext(Dispatchers.IO) {
            threadQueries.upsertThread(thread)
        }
    }

    override suspend fun saveThreads(threads: List<Thread>) {
        withContext(Dispatchers.IO) {
            threadQueries.transaction {
                threads.forEach { thread ->
                    threadQueries.upsertThread(thread)
                }
            }
        }
    }

    override suspend fun saveReplies(replies: List<ThreadReply>) {
        withContext(Dispatchers.IO) {
            threadReplyQueries.transaction {
                replies.forEach { reply ->
                    threadReplyQueries.upsertThreadReply(reply)
                }
            }
        }
    }

    override suspend fun clearForumCache(sourceId: String, fid: String) {
        withContext(Dispatchers.IO) {
            threadQueries.deleteThreadPage(sourceId, fid)
        }
    }

    override suspend fun clearThreadRepliesCache(sourceId: String, threadId: String) {
        withContext(Dispatchers.IO) {
            threadReplyQueries.deleteThreadRepliesByThreadId(sourceId, threadId)
        }
    }

    override suspend fun updateThreadLastAccessTime(sourceId: String, threadId: String, time: Long) {
        withContext(Dispatchers.IO) {
            threadQueries.updateThreadLastAccessTime(time, sourceId, threadId)
        }
    }

    override suspend fun updateThreadLastReadReplyId(sourceId: String, threadId: String, replyId: Long) {
        withContext(Dispatchers.IO) {
            threadQueries.updateThreadLastReadReplyId(replyId, sourceId, threadId)
        }
    }
}