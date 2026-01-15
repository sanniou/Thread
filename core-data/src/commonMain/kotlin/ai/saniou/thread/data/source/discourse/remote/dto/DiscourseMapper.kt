package ai.saniou.thread.data.source.discourse.remote.dto

import ai.saniou.thread.db.table.forum.Comment
import ai.saniou.thread.domain.model.forum.Author
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Comment as DomainComment


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
        agreeCount = 0,
        disagreeCount = 0,
        subCommentCount = 0,
        authorLevel = null,
        isPo = false
    )
}


internal fun DiscoursePost.toDomainComment(
    sourceId: String,
    threadId: String,
    page: Int,
): DomainComment {
    val replyCreatedAt = try {
        Instant.parse(createdAt)
    } catch (e: Exception) {
        Instant.fromEpochMilliseconds(0)
    }

    return DomainComment(
        id = id.toString(),
        sourceId = sourceId,
        topicId = threadId,
        author = Author(
            id = username,
            name = name ?: username,
            avatar = avatarTemplate.replace("{size}", "40"),
        ),
        createdAt = Instant.fromEpochMilliseconds(replyCreatedAt.toEpochMilliseconds()),
        content = cooked,
        floor = postNumber.toLong(),
        replyToId = replyToPostNumber?.toString(),
        agreeCount = 0,
        disagreeCount = 0,
        subCommentCount = 0,
        authorLevel = 0,
        isPo = false,
        images = emptyList(),
        isAdmin = false,
        title = null,
        subCommentsPreview = emptyList()
    )
}

