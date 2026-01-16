package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.db.table.forum.CommentQueries
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import ai.saniou.thread.db.table.forum.Comment
import ai.saniou.thread.db.table.forum.Topic
import kotlin.time.Clock

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
) : IBaseAuthor, IThreadBody

fun ThreadReply.toTableReply(
    sourceId: String,
    threadId: Long,
    page: Long = Long.MIN_VALUE,
    floor: Long = Long.MIN_VALUE,
) =
    Comment(
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
        floor = floor,
        agreeCount = 0,
        disagreeCount = 0,
        subCommentCount = 0,
        authorLevel = null,
        isPo = false,
        replyToId = null
    )

fun Thread.toTableReply(sourceId: String, page: Long) = this.replies.mapIndexed { index, it ->
    it.toTableReply(
        sourceId = sourceId,
        threadId = this.id,
        page = page,
    )
}

fun Thread.toTable(sourceId: String, page: Long) = Topic(
    id = id.toString(),
    sourceId = sourceId,
    channelId = fid.toString(),
    commentCount = replyCount,
    createdAt = now.toTime().toEpochMilliseconds(),
    authorId = userHash,
    authorName = name,
    title = title,
    content = content,
    summary = content, // NMB detail provides full content
    agreeCount = 0,
    disagreeCount = 0,
    isCollected = false,
    lastReplyAt = 0L,
    lastVisitedAt = null,
    lastViewedCommentId = null,
)

fun Thread.toCommentEntity(sourceId: String) = Comment(
    id = id.toString(),
    sourceId = sourceId,
    topicId = id.toString(),
    page = 1L,
    userHash = userHash,
    admin = admin,
    title = title,
    createdAt = now.toTime().toEpochMilliseconds(),
    content = content,
    authorName = name,
    floor = 1,
    agreeCount = 0,
    disagreeCount = 0,
    subCommentCount = 0,
    authorLevel = null,
    isPo = false,
    replyToId = null,
)
