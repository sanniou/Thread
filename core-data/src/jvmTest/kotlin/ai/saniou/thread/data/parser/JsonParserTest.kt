package ai.saniou.thread.data.parser

import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonParserTest {
    private val parser = JsonParser()
    private val source = FeedSource(
        id = "json",
        name = "Nested JSON",
        url = "https://example.com/api",
        type = FeedType.JSON,
        selectorConfig = mapOf(
            "itemsPath" to "data.items",
            "idPath" to "meta.id",
            "titlePath" to "attributes.title",
            "linkPath" to "attributes.link",
            "authorPath" to "relationships.author.name",
        ),
    )

    @Test
    fun parsesNestedDotPaths() = runBlocking {
        val content = """
            {"data":{"items":[{
              "meta":{"id":"42"},
              "attributes":{"title":"Architecture","link":"https://example.com/42"},
              "relationships":{"author":{"name":"Thread"}}
            }]}}
        """.trimIndent()

        val article = parser.parse(source, content).single()
        assertEquals("42", article.id)
        assertEquals("Architecture", article.title)
        assertEquals("Thread", article.author)
    }

    @Test
    fun invalidItemsPathIsAnObservableFailure() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { parser.parse(source, "{\"data\":{}}") }
        }
    }
}
