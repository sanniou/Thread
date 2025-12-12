package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.safety.Safelist
import kotlin.time.Clock

class HtmlParser : FeedParser {
    override suspend fun parse(source: FeedSource, content: String): List<Article> {
        val doc = Ksoup.parse(content, source.url) // Provide base URI for absolute URLs

        val containerSelector = source.selectorConfig["container"] ?: "body"
        val itemSelector = source.selectorConfig["item"] ?: "article"
        val titleSelector = source.selectorConfig["title"] ?: "h1, h2, h3"
        val linkSelector = source.selectorConfig["link"] ?: "a"
        val contentSelector = source.selectorConfig["content"] ?: "p"

        val container = doc.select(containerSelector).first() ?: doc
        val elements = container.select(itemSelector)

        return elements.mapNotNull { element ->
            val title = element.select(titleSelector).first()?.text()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val link = element.select(linkSelector).first()?.attr("abs:href")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val rawHtml = element.select(contentSelector).html()

            Article(
                id = link, // Use link as a unique ID
                feedSourceId = source.id,
                title = title,
                description = toPlainText(rawHtml).take(200),
                content = clean(rawHtml),
                rawContent = rawHtml,
                link = link,
                publishDate = Clock.System.now(), // HTML parsing usually doesn't give date easily
                isRead = false,
                isBookmarked = false
            )
        }
    }

    companion object {
        fun clean(html: String): String {
            // 允许基本格式化标签，移除危险标签
            val safelist = Safelist.relaxed()
                .addTags("img", "figure", "figcaption")
                .addAttributes("img", "src", "alt", "title")
            return Ksoup.clean(html, safelist) ?: ""
        }

        fun toPlainText(html: String): String {
            return Ksoup.parse(html).text()
        }
    }
}
