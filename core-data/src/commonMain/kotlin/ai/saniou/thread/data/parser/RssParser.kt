package ai.saniou.thread.data.parser

import ai.saniou.thread.data.parser.rss.Rss
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import com.fleeksoft.ksoup.Ksoup
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

class RssParser : FeedParser {
    private val xml = XML {
        recommended()
        defaultPolicy {
            ignoreUnknownChildren()
        }
    }

    override suspend fun parse(source: FeedSource, content: String): List<Article> {
        val rssArticles = runCatching {
            val rss = xml.decodeFromString(Rss.serializer(), content)
            rss.channel.items.mapNotNull { item ->
                val link = item.link ?: return@mapNotNull null
                val title = item.title ?: "No Title"

                val rawHtml = item.contentEncoded?.takeIf { it.isNotBlank() } ?: item.description ?: ""

                Article(
                    id = link,
                    feedSourceId = source.id,
                    title = title,
                    description = HtmlParser.toPlainText(rawHtml).take(200),
                    content = HtmlParser.clean(rawHtml),
                    rawContent = rawHtml,
                    link = link,
                    author = item.creator,
                    publishDate = item.pubDate?.let { parseRssDate(it) } ?: Clock.System.now(),
                    isRead = false,
                    isBookmarked = false
                )
            }
        }.getOrDefault(emptyList())
        if (rssArticles.isNotEmpty()) return rssArticles

        val document = Ksoup.parse(content, source.url)
        val atomArticles = document.select("entry").mapNotNull { entry ->
            val linkElement = entry.select("link").first()
            val link = linkElement?.attr("href")?.takeIf { it.isNotBlank() }
                ?: linkElement?.text()?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val title = entry.select("title").first()?.text()?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val rawHtml = entry.select("content").first()?.html()?.takeIf { it.isNotBlank() }
                ?: entry.select("summary").first()?.html().orEmpty()
            val date = entry.select("published").first()?.text()?.takeIf { it.isNotBlank() }
                ?: entry.select("updated").first()?.text()?.takeIf { it.isNotBlank() }
            Article(
                id = entry.select("id").first()?.text()?.takeIf { it.isNotBlank() } ?: link,
                feedSourceId = source.id,
                title = title,
                description = HtmlParser.toPlainText(rawHtml).take(200),
                content = HtmlParser.clean(rawHtml),
                rawContent = rawHtml,
                link = link,
                author = entry.select("author > name").first()?.text(),
                publishDate = date?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: Clock.System.now(),
                isRead = false,
                isBookmarked = false,
            )
        }
        require(atomArticles.isNotEmpty()) { "Invalid or empty RSS/Atom document" }
        return atomArticles
    }

    private val monthMap = mapOf(
        "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6,
        "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
    )

    private fun parseRssDate(date: String): Instant {
        // 尝试 ISO 8601 格式，这是 kotlinx-datetime 的标准格式
        try {
            return Instant.parse(date.trim())
        } catch (e: IllegalArgumentException) {
            // 不是 ISO 8601 格式，继续使用自定义解析器
        }

        // 尝试 RFC 1123/822 格式 (例如 "Wed, 02 Oct 2002 13:00:00 GMT")
        try {
            val parts = date.trim().split(' ').filter { it.isNotBlank() }
            // 预期至少有5个部分: Day, DD, MMM, YYYY, HH:MM:SS, [TZ]
            if (parts.size < 5) return Clock.System.now()

            // 星期几被忽略 (parts[0])
            val day = parts[1].toIntOrNull() ?: return Clock.System.now()
            val month = monthMap[parts[2]] ?: return Clock.System.now()
            val year = parts[3].toIntOrNull() ?: return Clock.System.now()

            val timeParts = parts[4].split(':')
            val hour = timeParts[0].toIntOrNull() ?: 0
            val minute = timeParts[1].toIntOrNull() ?: 0
            val second = timeParts.getOrNull(2)?.toIntOrNull() ?: 0

            val localDateTime = LocalDateTime(year, month, day, hour, minute, second)

            // 处理时区
            val timezonePart = if (parts.size > 5) parts[5] else "GMT"
            val offset = when {
                timezonePart.equals("GMT", ignoreCase = true) ||
                        timezonePart.equals("UT", ignoreCase = true) ||
                        timezonePart.equals("Z", ignoreCase = true) -> UtcOffset.ZERO

                timezonePart.startsWith("+") || timezonePart.startsWith("-") -> {
                    if (timezonePart.length == 5) {
                        val sign = if (timezonePart.startsWith("+")) 1 else -1
                        val hours = timezonePart.substring(1, 3).toIntOrNull() ?: 0
                        val minutes = timezonePart.substring(3, 5).toIntOrNull() ?: 0
                        UtcOffset(hours = sign * hours, minutes = sign * minutes)
                    } else {
                        UtcOffset.ZERO // 格式不正确，回退
                    }
                }
                // 可以添加更多时区缩写，但这会变得复杂。坚持使用偏移量和通用名称更安全。
                else -> UtcOffset.ZERO // 回退到 UTC
            }

            return localDateTime.toInstant(offset)

        } catch (_: Exception) {
            return Clock.System.now()
        }
    }
}
