package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.time.ExperimentalTime
import ai.saniou.thread.db.table.forum.Topic as EntityTopic

/**
 * Feed DTO
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Feed(
    override val id: Long,// 串的 ID
    @JsonNames("user_id")
    val userId: String? = null,// 发串的用户 ID？
    override val fid: Long,// 串所属的版面 ID
    @JsonNames("reply_count")
    override val replyCount: Long = 0,// 回复数量
    @JsonNames("recent_replies")
    val recentReplies: String? = null,// 最近几条回复的 ID，使用的是 [0,1,2,3] 这样的类似于数组的格式
    val category: String? = null,// ？
    @JsonNames("file_id")
    val fileId: String? = null,// ？
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    val email: String,
    override val title: String,
    override val content: String,
    val status: String? = null,// ？
    override val admin: Long,
    override val hide: Long = 0,
    val po: String? = null,// ？
    override val sage: Long = 0,
    val isLocal: Boolean = false,
) : IBaseThread


fun Feed.toTable(sourceId: String, page: Long) = EntityTopic(
    id = id.toString(),
    sourceId = sourceId,
    channelId = fid.toString(),
    commentCount = replyCount,
    createdAt = nowToEpochMilliseconds(), // now -> createdAt (Long)
    userHash = userHash,
    authorName = name,
    title = title,
    content = content,
    summary = content, // NMB feed provides full content
    sage = sage,
    admin = admin,
    hide = hide,
    page = page
)

@OptIn(ExperimentalTime::class)
fun Feed.nowToEpochMilliseconds(): Long {
    // 原始格式：2025-11-17(一)04:10:48
    // 1. 去掉括号和星期
    val cleaned = this.now.replace(Regex("\\(.*?\\)"), "")

    // cleaned: "2025-11-1704:10:48"
    // 2. 插入一个 T，变成 ISO-8601 兼容格式
    val isoString = cleaned.take(10) + "T" + cleaned.substring(10)

    // isoString: "2025-11-17T04:10:48"

    // 3. 解析成 LocalDateTime
    val ldt = LocalDateTime.parse(isoString)

    // 4. 转为 epoch milliseconds（系统所在时区）
    return ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun ForumThread.toDomain(): Topic {
    val author = Author(
        id = userHash,
        name = name,
        sourceName = "nmb"
    )
    val images = if (img.isNotBlank() && ext.isNotBlank()) {
        listOf(
            Image(
                originalUrl = "$img$ext",
                thumbnailUrl = "$img$ext",
                extension = ext
            )
        )
    } else {
        emptyList()
    }

    return Topic(
        id = id.toString(),
        sourceName = "nmb",
        sourceUrl = "https://nmb.ai/thread/$id",
        title = title,
        content = content,
        summary = content,
        author = author,
        createdAt = now.toTime(),
        channelId = fid.toString(), // channelId
        channelName = "",
        commentCount = replyCount, // commentCount
        images = images,
        isSage = sage > 0,
        isAdmin = admin > 0,
        isHidden = hide > 0,
        isLocal = false,
        // fixme  后续处理 lastReadCommentId 和 comments
        lastViewedCommentId = null,
        orderKey = (replies.maxOfOrNull { it.id } ?: id).toString(),
        comments = replies.map { it.toDomain(it.toString()) },
        remainingCount = remainingCount
    )
}

interface IBaseThread : IBaseAuthor, IThreadBody {

    val id: Long
    val sage: Long
    val fid: Long
    val replyCount: Long
    val title: String
    val admin: Long
    val hide: Long
}

interface IBaseAuthor {
    val now: String
    val userHash: String
    val name: String
}

interface IThreadBody {
    val content: String
    val img: String
    val ext: String
}

interface IBaseThreadReply {
    val replies: List<ThreadReply>
    val remainingCount: Long? // Remove or rename if needed, Post interface doesn't enforce it directly here
}
