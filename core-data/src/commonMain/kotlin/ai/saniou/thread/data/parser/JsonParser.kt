package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

class JsonParser : FeedParser {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    override suspend fun parse(source: FeedSource, content: String): List<Article> {
        return try {
            val jsonElement = json.parseToJsonElement(content)
            val config = source.selectorConfig

            val itemsPath = config["itemsPath"]
            val itemsArray = findJsonArray(jsonElement, itemsPath) ?: return emptyList()

            itemsArray.mapNotNull { itemElement ->
                if (itemElement !is JsonObject) return@mapNotNull null

                val id = itemElement.getString(config["idPath"])
                val title = itemElement.getString(config["titlePath"])
                val link = itemElement.getString(config["linkPath"])

                // ID 和 link 至少需要一个，title 是必须的
                if (id == null && link == null) return@mapNotNull null
                if (title == null) return@mapNotNull null

                val finalId = id ?: link!!
                val finalLink = link ?: id!!

                val contentText = itemElement.getString(config["contentPath"]) ?: ""

                Article(
                    id = finalId,
                    feedSourceId = source.id,
                    title = title,
                    description = contentText.take(200),
                    content = contentText,
                    link = finalLink,
                    author = itemElement.getString(config["authorPath"]),
                    publishDate = Clock.System.now(), // JSON 源通常没有标准日期字段
                    imageUrl = itemElement.getString(config["imagePath"]),
                    rawContent = itemElement.toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun findJsonArray(element: JsonElement, path: String?): JsonArray? {
        if (path.isNullOrBlank() || path == "$") {
            return element as? JsonArray
        }
        // 目前只支持根路径访问。更复杂的实现可以解析点分隔的路径。
        return (element as? JsonObject)?.get(path) as? JsonArray
    }

    private fun JsonObject.getString(key: String?): String? {
        if (key.isNullOrBlank()) return null
        // 目前尚不支持 "user.name" 这样的嵌套路径。
        // 对于当前需求，这已足够。
        return this[key]?.jsonPrimitive?.content
    }
}