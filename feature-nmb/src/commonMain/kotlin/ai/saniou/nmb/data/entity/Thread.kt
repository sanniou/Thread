package ai.saniou.nmb.data.entity

import ai.saniou.nmb.workflow.thread.ThreadReply
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Thread(
    val id: Long,
    val fid: Long,
    @JsonNames("ReplyCount")
    val replyCount: Long,
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    val title: String,
    override val content: String,
    val sage: Long,
    val admin: Long,
    @JsonNames("Hide")
    val hide: Long,
    @JsonNames("Replies")
    val replies: List<ThreadReply>,
) : IBaseAuthor, IThreadBody

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
) : IBaseAuthor, IThreadBody

fun ThreadReply.toTable(threadId: Long, indexInThread: Long = Long.MIN_VALUE) =
    ai.saniou.nmb.db.table.ThreadReply(
        id = this.id,
        userHash = this.userHash,
        admin = this.admin,
        title = this.title,
        now = this.now,
        content = this.content,
        img = this.img,
        ext = this.ext,
        name = this.name,
        threadId = threadId,
        indexInThread = indexInThread,
    )

fun Thread.toTableReply(page: Long) = this.replies.mapIndexed { index, it ->
    it.toTable(
        threadId = this.id,
        indexInThread = (page - 1) * 20 + index,
    )
}

fun ai.saniou.nmb.db.table.ThreadReply.toThreadReply() = ThreadReply(
    id = id,
    userHash = userHash,
    admin = admin,
    title = title,
    now = now,
    content = content,
    img = img,
    ext = ext,
    name = name,
)

fun ai.saniou.nmb.db.table.Thread.toThread() = Thread(
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
    replies = emptyList()
)


fun Thread.toTable() = ai.saniou.nmb.db.table.Thread(
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
)
