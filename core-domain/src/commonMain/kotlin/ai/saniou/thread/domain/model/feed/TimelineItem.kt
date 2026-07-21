package ai.saniou.thread.domain.model.feed

import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.social.SocialPost
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface TimelineItem {
    val uniqueId: String
    val displayTime: Instant
}

data class PostItem(
    val post: Topic,
) : TimelineItem {
    override val uniqueId: String = "post_${post.sourceId}_${post.id}"

    @OptIn(ExperimentalTime::class)
    override val displayTime: Instant =
        Instant.fromEpochMilliseconds(post.createdAt.toEpochMilliseconds())
}

data class ArticleItem(
    val article: Article,
    val sourceName: String,
    val sourceIconUrl: String?,
) : TimelineItem {
    override val uniqueId: String = "article_${article.id}"
    override val displayTime: Instant = article.publishDate
}

data class SocialItem(
    val post: SocialPost,
    val sourceName: String,
) : TimelineItem {
    override val uniqueId: String = "social_${post.sourceId}_${post.id}"
    override val displayTime: Instant = Instant.fromEpochMilliseconds(post.createdAtEpochMillis)
}
