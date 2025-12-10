package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.FeedType

class FeedParserFactory(
    private val rssParser: RssParser,
    private val jsonParser: JsonParser,
    private val htmlParser: HtmlParser
) {
    fun getParser(type: FeedType): FeedParser {
        return when (type) {
            FeedType.RSS -> rssParser
            FeedType.JSON -> jsonParser
            FeedType.HTML -> htmlParser
        }
    }
}