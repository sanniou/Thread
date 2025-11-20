package ai.saniou.nmb.data.entity

import ai.saniou.nmb.db.table.GetHistoryThreads
import ai.saniou.nmb.db.table.GetThreadsInForum
import ai.saniou.nmb.db.table.GetThreadsInForumOffset
import ai.saniou.nmb.db.table.ThreadReplyQueries
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
    override val remainReplies: Long?,
    val lastKey: Long,
    val last_access_time: Long,
    val last_read_reply_id: Long,
) : IBaseThread, IThreadBody, IBaseThreadReply

fun GetThreadsInForum.toThreadWithInformation(query: ThreadReplyQueries? = null) =
    ThreadWithInformation(
        id = id,
        fid = fid,
        replyCount = replyCount,
        img = img,
        ext = ext,
        now = now,
        userHash = userHash,
        name = name,
        title = title,
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = query?.getLastFiveReplies(id)?.executeAsList()?.map {
            it.toThreadReply()
        } ?: emptyList(),
        remainReplies = remainReplies,
        lastKey = lastKey!!,
        last_access_time = last_access_time!!,
        last_read_reply_id = last_read_reply_id!!,
    )

fun GetThreadsInForumOffset.toThreadWithInformation(query: ThreadReplyQueries? = null) =
    ThreadWithInformation(
        id = id,
        fid = fid,
        replyCount = replyCount,
        img = img,
        ext = ext,
        now = now,
        userHash = userHash,
        name = name,
        title = title,
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = query?.getLastFiveReplies(id)?.executeAsList()?.map {
            it.toThreadReply()
        } ?: emptyList(),
        remainReplies = remainReplies,
        lastKey = lastKey!!,
        last_access_time = last_access_time!!,
        last_read_reply_id = last_read_reply_id!!,
    )

fun GetHistoryThreads.toThreadWithInformation(query: ThreadReplyQueries? = null) =
    ThreadWithInformation(
        id = id,
        fid = fid,
        replyCount = replyCount,
        img = img,
        ext = ext,
        now = now,
        userHash = userHash,
        name = name,
        title = title,
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = query?.getLastFiveReplies(id)?.executeAsList()?.map {
            it.toThreadReply()
        } ?: emptyList(),
        remainReplies = remainReplies,
        lastKey = lastKey,
        last_access_time = last_access_time,
        last_read_reply_id = last_read_reply_id
    )
