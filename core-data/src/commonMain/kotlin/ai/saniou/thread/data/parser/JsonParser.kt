package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock

class JsonParser : FeedParser {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    override suspend fun parse(source: FeedSource, content: String): List<Article> {
        val jsonElement = json.parseToJsonElement(content)
        val config = source.selectorConfig
        require(!config["titlePath"].isNullOrBlank()) { "JSON titlePath is required" }
        require(!config["linkPath"].isNullOrBlank() || !config["idPath"].isNullOrBlank()) {
            "JSON linkPath or idPath is required"
        }

        val itemsPath = config["itemsPath"]
        val itemsArray = findPath(jsonElement, itemsPath) as? JsonArray
            ?: throw IllegalArgumentException("JSON itemsPath does not point to an array")

        val articles = itemsArray.mapNotNull { itemElement ->
                if (itemElement !is JsonObject) return@mapNotNull null

                val id = itemElement.getStringAt(config["idPath"])
                val title = itemElement.getStringAt(config["titlePath"])
                val link = itemElement.getStringAt(config["linkPath"])

                // ID 和 link 至少需要一个，title 是必须的
                if (id == null && link == null) return@mapNotNull null
                if (title == null) return@mapNotNull null

                val finalId = id ?: link!!
                val finalLink = link ?: id!!

                val contentText = itemElement.getStringAt(config["contentPath"]) ?: ""

                Article(
                    id = finalId,
                    feedSourceId = source.id,
                    title = title,
                    description = contentText.take(200),
                    content = contentText,
                    link = finalLink,
                    author = itemElement.getStringAt(config["authorPath"]),
                    publishDate = Clock.System.now(), // JSON 源通常没有标准日期字段
                    imageUrl = itemElement.getStringAt(config["imagePath"]),
                    rawContent = itemElement.toString()
                )
        }
        require(articles.isNotEmpty()) { "JSON feed contains no valid items" }
        return articles
    }

    private fun findPath(element: JsonElement, path: String?): JsonElement? {
        if (path.isNullOrBlank() || path == "$") {
            return element
        }
        return path.removePrefix("$.").split('.').filter(String::isNotBlank).fold(element as JsonElement?) { current, segment ->
            when (current) {
                is JsonObject -> current[segment]
                is JsonArray -> segment.toIntOrNull()?.let(current::getOrNull)
                else -> null
            }
        }
    }

    private fun JsonObject.getStringAt(path: String?): String? =
        path?.takeIf { it.isNotBlank() }
            ?.let { findPath(this, it) as? JsonPrimitive }
            ?.contentOrNull
}
