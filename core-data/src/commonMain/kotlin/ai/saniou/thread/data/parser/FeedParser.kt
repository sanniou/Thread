package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource

interface FeedParser {
    suspend fun parse(source: FeedSource, content: String): List<Article>
}