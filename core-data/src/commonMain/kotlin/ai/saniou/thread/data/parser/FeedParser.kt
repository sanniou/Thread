package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource

interface FeedParser {
    suspend fun parse(source: FeedSource, content: String): List<Article>
}