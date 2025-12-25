package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset as GetThreadsInForumOffset
import ai.saniou.thread.db.table.forum.SearchTopics as SearchThreads
import ai.saniou.thread.db.table.forum.Topic as Thread
import ai.saniou.thread.db.table.forum.CommentQueries as ThreadReplyQueries
import kotlinx.serialization.json.JsonNames

data class ThreadWithInformation(
    override val id: Long,
    override val fid: Long,
    @JsonNames("ReplyCount")
    override val replyCount: Long,
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    override val title: String,
    override val content: String,
    override val sage: Long,
    override val admin: Long,
    @JsonNames("Hide")
    override val hide: Long,
    @JsonNames("Replies")
    override val replies: List<ThreadReply>,
    override val remainingCount: Long?,
    val lastKey: Long?,
    val last_access_time: Long?,
    val last_read_reply_id: Long?,
) : IBaseThread, IThreadBody, IBaseThreadReply

fun Thread.toThreadWithInformation(query: ThreadReplyQueries? = null) =
    ThreadWithInformation(
        id = id.toLong(),
        fid = channelId.toLong(),
        replyCount = commentCount,
        img = "",
        ext = "",
        now = createdAt.toString(),
        userHash = userHash,
        name = authorName,
        title = title ?: "",
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toThreadReply()
        } ?: emptyList(),
        remainingCount = null,
        lastKey = null,
        last_access_time = null,
        last_read_reply_id = null,
    )

fun SearchThreads.toThreadWithInformation(query: ThreadReplyQueries? = null) =
    ThreadWithInformation(
        id = id.toLong(),
        fid = channelId.toLong(),
        replyCount = commentCount,
        img = "",
        ext = "",
        now = createdAt.toString(),
        userHash = userHash,
        name = authorName,
        title = title ?: "",
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toThreadReply()
        } ?: emptyList(),
        remainingCount = remainingCount,
        lastKey = lastKey,
        last_access_time = last_access_time,
        last_read_reply_id = last_read_comment_id,
    )

fun GetThreadsInForumOffset.toThreadWithInformation(query: ThreadReplyQueries? = null) =
    ThreadWithInformation(
        id = id.toLong(),
        fid = channelId.toLong(),
        replyCount = commentCount,
        img = "",
        ext = "",
        now = createdAt.toString(),
        userHash = userHash,
        name = authorName,
        title = title ?: "",
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = query?.getLastFiveComments(sourceId, id)?.executeAsList()?.map {
            it.toThreadReply()
        } ?: emptyList(),
        remainingCount = remainingCount,
        lastKey = lastKey,
        last_access_time = last_access_time,
        last_read_reply_id = last_read_comment_id,
    )
