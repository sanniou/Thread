package ai.saniou.thread.data.mapper

import ai.saniou.thread.db.table.forum.ImageQueries
import ai.saniou.thread.db.table.forum.CommentQueries
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.ImageType
import kotlin.time.Instant
import ai.saniou.thread.db.table.forum.Comment as EntityComment

fun Comment.toEntity(sourceId: String, page: Long, floor: Long): EntityComment {
    return EntityComment(
        id = id,
        sourceId = sourceId,
        topicId = topicId,
        page = page,
        userHash = author.id,
        admin = if (isAdmin) 1 else 0,
        title = title,
        createdAt = createdAt.toEpochMilliseconds(),
        content = content,
        authorName = author.name,
        floor = floor,
        replyToId = replyToId,
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        subCommentCount = subCommentCount.toLong(),
        authorLevel = authorLevel?.toLong(),
        isPo = isPo
    )
}

fun EntityComment.toDomain(
    imageQueries: ImageQueries? = null,
    commentQueries: CommentQueries? = null,
    includeSubComments: Boolean = true,
): Comment {
    val images = imageQueries?.getImagesByParent(
        sourceId = sourceId,
        parentId = id,
        parentType = ImageType.Comment
    )?.executeAsList()?.map { it.toDomain() }
        ?: emptyList()

    return Comment(
        id = id,
        topicId = topicId,
        author = Author(
            id = userHash,
            name = authorName,
            sourceName = sourceId,
        ),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        title = title,
        content = content,
        images = images,
        isAdmin = admin > 0,
        floor = floor,
        replyToId = replyToId,
        sourceId = sourceId,
        agreeCount = agreeCount,
        disagreeCount = disagreeCount,
        subCommentCount = subCommentCount.toInt(),
        authorLevel = authorLevel?.toInt(),
        isPo = isPo,
        subCommentsPreview = if (includeSubComments) {
            commentQueries?.getSubCommentPreviews(sourceId, id, 3)
                ?.executeAsList()
                ?.map { it.toDomain(imageQueries, commentQueries = null, includeSubComments = false) }
                .orEmpty()
        } else {
            emptyList()
        }
    )
}
