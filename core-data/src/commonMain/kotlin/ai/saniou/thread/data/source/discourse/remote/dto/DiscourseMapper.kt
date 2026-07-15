package ai.saniou.thread.data.source.discourse.remote.dto

import ai.saniou.thread.domain.model.forum.Author
import kotlin.time.Instant
import ai.saniou.thread.domain.model.forum.Comment as DomainComment


internal fun DiscoursePost.toDomainComment(
    sourceId: String,
    sourceName: String,
    threadId: String,
    sourceBaseUrl: String = "",
    replyTargetId: String? = null,
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
            avatar = avatarTemplate.replace("{size}", "80").resolveDiscourseUrl(sourceBaseUrl),
            sourceName = sourceName,
        ),
        createdAt = Instant.fromEpochMilliseconds(replyCreatedAt.toEpochMilliseconds()),
        content = cooked,
        floor = postNumber.toLong(),
        replyToId = replyTargetId,
        agreeCount = null,
        disagreeCount = 0,
        subCommentCount = 0,
        authorLevel = trustLevel,
        isPo = postNumber == 1,
        images = cooked.extractDiscourseImages(sourceBaseUrl),
        isAdmin = admin || moderator,
        title = null,
        subCommentsPreview = emptyList()
    )
}

internal fun String.extractDiscourseImages(baseUrl: String): List<ai.saniou.thread.domain.model.forum.Image> =
    Regex("""<img[^>]+(?:data-src|src)=[\"']([^\"']+)[\"'][^>]*>""", RegexOption.IGNORE_CASE)
        .findAll(this)
        .map { it.groupValues[1].resolveDiscourseUrl(baseUrl) }
        .filter { it.isNotBlank() && !it.startsWith("data:") }
        .distinct()
        .map { url ->
            ai.saniou.thread.domain.model.forum.Image(
                originalUrl = url,
                thumbnailUrl = url,
                extension = url.substringBefore('?').substringAfterLast('.', "").takeIf(String::isNotBlank),
            )
        }
        .toList()

internal fun String.resolveDiscourseUrl(baseUrl: String): String = when {
    isBlank() -> this
    startsWith("https://") || startsWith("http://") -> this
    startsWith("//") -> "https:$this"
    else -> baseUrl.trimEnd('/') + "/" + trimStart('/')
}
