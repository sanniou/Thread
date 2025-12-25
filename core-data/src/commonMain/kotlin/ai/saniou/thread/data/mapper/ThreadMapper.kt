package ai.saniou.thread.data.mapper


import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.db.table.forum.Thread
import ai.saniou.thread.domain.model.forum.Post
import kotlin.time.Instant

fun Thread.toDomain(): Post = Post(
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
    isLocal = false,
    now = now ?: "",
    name = name ?: "",
    sage = sage ?: 0,
    fid = fid?.toLongOrNull() ?: 0,
    admin = admin ?: 0,
    hide = hide ?: 0,
    // fixme  后续处理 lastReadReplyId 和 replies
    lastReadReplyId = "",
    replies = null,
    remainReplies = null
)

fun Post.toEntity(page: Int = 1): Thread {
    return Thread(
        id = id,
        sourceId = sourceName,
        fid = fid.toString(),
        replyCount = replyCount,
        img = img ?: "",
        ext = ext ?: "",
        now = now,
        userHash = userHash,
        name = name,
        title = title,
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        page = page.toLong()
    )
}
