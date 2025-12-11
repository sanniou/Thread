package ai.saniou.thread.data.parser

import ai.saniou.thread.data.parser.rss.Rss
import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.time.Clock
import kotlin.time.Instant

class RssParser : FeedParser {
    private val xml = XML {
        recommended()
        defaultPolicy {
            ignoreUnknownChildren()
        }
    }

    override suspend fun parse(source: FeedSource, content: String): List<Article> {
        return try {
            val rss = xml.decodeFromString(Rss.serializer(), content)
            rss.channel.items.mapNotNull { item ->
                val link = item.link ?: return@mapNotNull null
                val title = item.title ?: "No Title"

                Article(
                    id = link,
                    feedSourceId = source.id,
                    title = title,
                    content = item.description ?: "",
                    link = link,
                    author = item.creator,
                    publishDate = item.pubDate?.let { parseRssDate(it) } ?: Clock.System.now(),
                    isRead = false,
                    isBookmarked = false
                )
            }
        } catch (e: Exception) {
            // Log error
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseRssDate(date: String): Instant {
        // TODO: Implement a robust RSS date parser
        // This is a simplified placeholder
        return Clock.System.now()
    }
}
