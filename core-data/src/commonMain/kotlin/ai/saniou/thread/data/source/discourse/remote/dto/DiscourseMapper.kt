package ai.saniou.thread.data.source.discourse.remote.dto

import ai.saniou.thread.db.table.forum.Comment
import kotlin.time.Instant


internal fun DiscoursePost.toComment(sourceId: String, threadId: String, page: Int): Comment {
    val replyCreatedAt = try {
        Instant.parse(createdAt)
    } catch (e: Exception) {
        Instant.fromEpochMilliseconds(0)
    }

    return Comment(
        id = id.toString(),
        sourceId = sourceId,
        topicId = threadId,
        page = page.toLong(),
        userHash = username,
        admin = 0, // TODO: Map admin status
        title = null,
        createdAt = replyCreatedAt.toEpochMilliseconds(),
        content = cooked,
        authorName = name ?: username,
        floor = postNumber.toLong(),
        replyToId = replyToPostNumber?.toString(),
    )
}
