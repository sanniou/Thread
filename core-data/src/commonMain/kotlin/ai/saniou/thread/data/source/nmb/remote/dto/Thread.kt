package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.db.table.forum.CommentQueries
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import ai.saniou.thread.db.table.forum.Comment as TableComment
import ai.saniou.thread.db.table.forum.Topic as TableTopic

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

fun ThreadReply.toTableReply(sourceId: String, threadId: Long, page: Long = Long.MIN_VALUE) =
    TableComment(
        id = this.id.toString(),
        sourceId = sourceId,
        userHash = this.userHash,
        admin = this.admin,
        title = this.title,
        createdAt = now.toTime().toEpochMilliseconds(),
        content = this.content,
        // img/ext removed, need to save to Image table
        authorName = this.name,
        topicId = threadId.toString(),
        page = page,
        floor = null,
        replyToId = null
    )

fun Thread.toTableReply(sourceId: String, page: Long) = this.replies.mapIndexed { index, it ->
    it.toTableReply(
        sourceId = sourceId,
        threadId = this.id,
        page = page,
    )
}

fun TableComment.toThreadReply() = ThreadReply(
    id = id.toLongOrNull() ?: 0L,
    userHash = userHash,
    admin = admin,
    title = title ?: "",
    now = createdAt.toString(), // FIXME: Format timestamp back to string if needed
    content = content,
    img = "", // Image handled separately
    ext = "",
    name = authorName,
    threadId = topicId.toLongOrNull() ?: 0L,
)

fun TableTopic.toThread(query: CommentQueries? = null) = Thread(
    id = id.toLongOrNull() ?: 0L,
    fid = channelId.toLongOrNull() ?: 0L,
    replyCount = commentCount,
    img = "", // Image handled separately
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
    } ?: emptyList()
)


fun Thread.toTable(sourceId: String, page: Long) = TableTopic(
    id = id.toString(),
    sourceId = sourceId,
    channelId = fid.toString(),
    commentCount = replyCount,
    createdAt = now.toTime().toEpochMilliseconds(),
    userHash = userHash,
    authorName = name,
    title = title,
    content = content,
    sage = sage,
    admin = admin,
    hide = hide,
    page = page,
)
