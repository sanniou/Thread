package ai.saniou.thread.domain.model.history

import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.model.reader.Article
import kotlinx.datetime.Instant

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