package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.thread.db.table.forum.ThreadReplyQueries
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Thread(
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
    val replies: List<ThreadReply>,
) : IBaseThread, IThreadBody

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ThreadReply(
    val id: Long,
    @JsonNames("user_hash")
    override val userHash: String,
    val admin: Long,
    val title: String,
    override val now: String,
    override val content: String,
    override val img: String,
    override val ext: String,
    override val name: String,
    val threadId: Long = 0,
) : IBaseAuthor, IThreadBody

fun ThreadReply.toTable(sourceId: String, threadId: Long, page: Long = Long.MIN_VALUE) =
    ai.saniou.thread.db.table.forum.ThreadReply(
        id = this.id.toString(),
        sourceId = sourceId,
        userHash = this.userHash,
        admin = this.admin,
        title = this.title,
        now = this.now,
        content = this.content,
        img = this.img,
        ext = this.ext,
        name = this.name,
        threadId = threadId.toString(),
        page = page,
    )

fun Thread.toTableReply(sourceId: String, page: Long) = this.replies.mapIndexed { index, it ->
    it.toTable(
        sourceId = sourceId,
        threadId = this.id,
        page = page,
    )
}

fun ai.saniou.thread.db.table.forum.ThreadReply.toThreadReply() = ThreadReply(
    id = id.toLongOrNull() ?: 0L,
    userHash = userHash,
    admin = admin,
    title = title,
    now = now,
    content = content,
    img = img,
    ext = ext,
    name = name,
    threadId = threadId.toLongOrNull() ?: 0L,
)

fun ai.saniou.thread.db.table.forum.Thread.toThread(query: ThreadReplyQueries? = null) = Thread(
    id = id.toLongOrNull() ?: 0L,
    fid = fid.toLongOrNull() ?: 0L,
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
    replies = query?.getLastFiveReplies(sourceId, id)?.executeAsList()?.map {
        it.toThreadReply()
    } ?: emptyList()
)


fun Thread.toTable(sourceId: String, page: Long) = ai.saniou.thread.db.table.forum.Thread(
    id = id.toString(),
    sourceId = sourceId,
    fid = fid.toString(),
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
    page = page,
)
