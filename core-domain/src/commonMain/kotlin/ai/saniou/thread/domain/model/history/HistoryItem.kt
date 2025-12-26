package ai.saniou.thread.domain.model.history

import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.model.reader.Article
import kotlin.time.Instant

sealed interface HistoryItem {
    val uniqueId: String
    val accessTime: Instant
}

data class HistoryPost(
    val post: Post,
    override val accessTime: Instant
) : HistoryItem {
    override val uniqueId: String = "post_${post.sourceName}_${post.id}"
}

data class HistoryArticle(
    val article: Article,
    override val accessTime: Instant
) : HistoryItem {
    override val uniqueId: String = "article_${article.id}"
}
