package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource

class JsonParser : FeedParser {
    override suspend fun parse(source: FeedSource, content: String): List<Article> {
         // TODO: Implement actual JSON parsing
         // This is a placeholder
        return emptyList()
    }
}