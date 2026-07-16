package ai.saniou.thread.data.reader

import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReaderSubscriptionCodecTest {
    private val codec = ReaderSubscriptionCodec()
    private val sources = listOf(
        FeedSource(
            id = "example-json",
            name = "Example & News",
            url = "https://example.com/api/news",
            type = FeedType.JSON,
            selectorConfig = mapOf(
                "itemsPath" to "data.items",
                "titlePath" to "attributes.title",
                "linkPath" to "attributes.url",
            ),
            refreshInterval = 900_000,
        ),
        FeedSource(
            id = "example-rss",
            name = "Example RSS",
            url = "https://example.com/feed.xml?lang=zh&full=1",
            type = FeedType.RSS,
            autoRefresh = false,
        ),
    )

    @Test
    fun jsonRoundTripKeepsCommonConfiguration() {
        val payload = codec.encode(sources, ReaderSubscriptionFormat.JSON)
        val restored = codec.decode(payload, ReaderSubscriptionFormat.JSON)

        assertEquals(sources, restored)
        assertTrue(payload.contains("thread-reader-subscriptions"))
    }

    @Test
    fun opmlRoundTripEscapesXmlAndKeepsThreadMetadata() {
        val rss = sources.last()
        val payload = codec.encode(listOf(rss), ReaderSubscriptionFormat.OPML)
        val restored = codec.decode(payload, ReaderSubscriptionFormat.OPML).single()

        assertEquals(rss.id, restored.id)
        assertEquals(rss.name, restored.name)
        assertEquals(rss.url, restored.url)
        assertEquals(rss.autoRefresh, restored.autoRefresh)
        assertTrue(payload.contains("&amp;"))
    }

    @Test
    fun rejectsDuplicateUrlsBeforeRepositoryMutation() {
        val payload = """
            {
              "format":"thread-reader-subscriptions",
              "version":1,
              "sources":[
                {"id":"a","name":"A","url":"https://example.com/feed","type":"RSS"},
                {"id":"b","name":"B","url":"https://example.com/feed","type":"RSS"}
              ]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            codec.decode(payload, ReaderSubscriptionFormat.JSON)
        }
    }
}
