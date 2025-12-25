package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.thread.db.table.forum.Forum
import ai.saniou.thread.db.table.forum.GetThread
import ai.saniou.thread.db.table.forum.SelectSubscriptionThread
import ai.saniou.thread.db.table.forum.Thread
import ai.saniou.thread.db.table.forum.ThreadReply
import ai.saniou.thread.db.table.forum.TimeLine
import ai.saniou.thread.domain.model.forum.Post
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Forum as DomainForum

fun TimeLine.toDomain(): DomainForum = DomainForum(
    id = id.toString(),
    name = name,
    sourceName = "nmb",
    tag = "timeline",
    showName = displayName,
    msg = notice,
    groupId = "-1",
    groupName = "TimeLine",
    threadCount = maxPage?.let { it * 20 }, // Approximate thread count
    autoDelete = null,
)


@OptIn(ExperimentalTime::class)
fun SelectSubscriptionThread.toDomain(): Post = Post(
    id = id,
    sourceName = "nmb",
    sourceUrl = "https://nmb.ai/thread/$id",
    title = title ?: "",
    content = content ?: "",
    author = name ?: "",
    userHash = userHash ?: "",
    createdAt = now?.toTime() ?: Instant.fromEpochMilliseconds(0),
    forumName = fid ?: "",
    replyCount = replyCount ?: 0,
    img = img,
    ext = ext,
    isSage = (sage ?: 0) > 0,
    isAdmin = (admin ?: 0) > 0,
    isHidden = (hide ?: 0) > 0,
    isLocal = isLocal == 1L,
    now = now ?: "",
    name = name ?: "",
    sage = sage ?: 0,
    fid = fid?.toLongOrNull() ?: 0,
    admin = admin ?: 0,
    hide = hide ?: 0,
    // fixme  后续处理 lastReadReplyId
    lastReadReplyId = "",
    replies = null, // SelectSubscriptionThread doesn't join with replies
    remainReplies = null
)

@OptIn(ExperimentalTime::class)
fun ThreadWithInformation.toDomain(): Post = Post(
    id = id.toString(),
    sourceName = "nmb",
    sourceUrl = "https://nmb.ai/thread/$id",
    title = title ?: "",
    content = content ?: "",
    author = name ?: "",
    userHash = userHash ?: "",
    createdAt = now?.toTime() ?: Instant.fromEpochMilliseconds(0),
    forumName = fid.toString(),
    replyCount = replyCount ?: 0,
    img = img,
    ext = ext,
    isSage = (sage ?: 0) > 0,
    isAdmin = (admin ?: 0) > 0,
    isHidden = (hide ?: 0) > 0,
    isLocal = true,
    now = now ?: "",
    name = name ?: "",
    sage = sage ?: 0,
    fid = fid ?: 0,
    admin = admin ?: 0,
    hide = hide ?: 0,
    // fixme  后续处理 lastReadReplyId
    lastReadReplyId = "",
    replies = replies.map { it.toDomain() }.reversed(),
    remainReplies = remainReplies
)

@OptIn(ExperimentalTime::class)
fun GetThread.toDomain(): Post = Post(
    id = id,
    sourceName = sourceId ?: "nmb",
    sourceUrl = "https://nmb.ai/thread/$id",
    title = title ?: "",
    content = content ?: "",
    author = name ?: "",
    userHash = userHash ?: "",
    createdAt = now?.toTime() ?: Instant.fromEpochMilliseconds(0),
    forumName = fid ?: "",
    replyCount = replyCount ?: 0,
    img = img,
    ext = ext,
    isSage = (sage ?: 0) > 0,
    isAdmin = (admin ?: 0) > 0,
    isHidden = (hide ?: 0) > 0,
    isLocal = true,
    now = now ?: "",
    name = name ?: "",
    sage = sage ?: 0,
    fid = fid?.toLongOrNull() ?: 0,
    admin = admin ?: 0,
    hide = hide ?: 0,
    // fixme  后续处理 lastReadReplyId
    lastReadReplyId = "",
    replies = null, // GetThread doesn't join with replies
    remainReplies = null
)

@OptIn(ExperimentalTime::class)
fun String.toTime(): Instant {
    val input = this.trim()
    // If it's already in ISO format with 'T', parse directly
    if (input.contains("T")) {
        return try {
            // Try parsing as standard ISO format first
            Instant.parse(input)
        } catch (e: IllegalArgumentException) {
            // If that fails, try to clean up double T issue
            val cleanedInput = input.replace(Regex("T{2,}"), "T")
            try {
                Instant.parse(cleanedInput)
            } catch (e2: IllegalArgumentException) {
                // If still fails, try to parse as local datetime
                val isoString = if (cleanedInput.length > 11) {
                    cleanedInput.take(10) + "T" + cleanedInput.substring(11)
                } else {
                    cleanedInput
                }
                LocalDateTime.parse(isoString).toInstant(TimeZone.currentSystemDefault())
            }
        }
    }

    // 原始格式：2025-11-17(一)04:10:48
    // 1. 去掉括号和星期
    val cleaned = input.replace(Regex("\\(.*?\\)"), "")

    // cleaned: "2025-11-1704:10:48"
    // 2. 插入一个 T，变成 ISO-8601 兼容格式
    val isoString = cleaned.take(10) + "T" + cleaned.substring(10)

    // isoString: "2025-11-17T04:10:48"

    // 3. 解析成 LocalDateTime
    val ldt = LocalDateTime.parse(isoString)

    // 4. 转为 epoch milliseconds（系统所在时区）
    return ldt.toInstant(TimeZone.currentSystemDefault())
}

@OptIn(ExperimentalTime::class)
fun ai.saniou.thread.data.source.nmb.remote.dto.Thread.toDomain(): Post = Post(
    id = id.toString(),
    sourceName = "nmb",
    sourceUrl = "https://nmb.ai/thread/$id",
    title = title,
    content = content,
    author = name,
    userHash = userHash,
    createdAt = now.toTime(),
    forumName = fid.toString(),
    replyCount = replyCount,
    img = img,
    ext = ext,
    isSage = sage > 0,
    isAdmin = admin > 0,
    isHidden = hide > 0,
    isLocal = false,
    now = now,
    name = name,
    sage = sage,
    fid = fid,
    admin = admin,
    hide = hide,
    lastReadReplyId = "",
    replies = replies.map { it.toDomain() },
    remainReplies = (replyCount - replies.size).coerceAtLeast(0)
)

@OptIn(ExperimentalTime::class)
fun ThreadReply.toDomain(): ai.saniou.thread.domain.model.forum.ThreadReply =
    ai.saniou.thread.domain.model.forum.ThreadReply(
        id = id,
        userHash = userHash ?: "",
        admin = admin ?: 0,
        title = title ?: "",
        now = now ?: "",
        createdAt = now?.toTime() ?: Instant.fromEpochMilliseconds(0),
        content = content ?: "",
        img = img ?: "",
        ext = ext ?: "",
        name = name ?: "",
        threadId = threadId ?: ""
    )

@OptIn(ExperimentalTime::class)
fun ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply.toDomain(): ai.saniou.thread.domain.model.forum.ThreadReply =
    ai.saniou.thread.domain.model.forum.ThreadReply(
        id = id.toString(),
        userHash = userHash,
        admin = admin,
        title = title,
        now = now,
        createdAt = now.toTime(),
        content = content,
        img = img,
        ext = ext,
        name = name,
        threadId = threadId.toString()
    )
