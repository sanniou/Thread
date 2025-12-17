package ai.saniou.thread.data.mapper

import ai.saniou.thread.db.table.reader.ArticleEntity
import ai.saniou.thread.domain.model.reader.Article
import kotlin.time.Instant

fun ArticleEntity.toDomain() = Article(
    id = id,
    feedSourceId = feedSourceId,
    title = title,
    description = description,
    content = content,
    link = link,
    author = author,
    publishDate = Instant.fromEpochMilliseconds(publishDate),
    isRead = isRead == 1L,
    isBookmarked = isBookmarked == 1L,
    imageUrl = imageUrl,
    rawContent = rawContent
)
