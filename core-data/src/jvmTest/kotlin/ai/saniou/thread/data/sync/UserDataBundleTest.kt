package ai.saniou.thread.data.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UserDataBundleTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun versionedBundleRoundTrips() {
        val bundle = UserDataBundle(
            exportedAtEpochMillis = 123,
            feedSources = listOf(
                FeedSourceSnapshot("rss", "RSS", "https://example.com/rss", "RSS"),
            ),
            articleStates = listOf(ArticleStateSnapshot("article", true, false, 456)),
        )

        val payload = json.encodeToString(bundle)
        val restored = json.decodeFromString<UserDataBundle>(payload)

        assertEquals(bundle, restored)
        assertTrue(payload.contains("thread-user-data"))
    }

    @Test
    fun rejectsFutureBundleVersion() {
        assertFailsWith<IllegalArgumentException> {
            json.decodeFromString<UserDataBundle>(
                "{\"format\":\"thread-user-data\",\"version\":99,\"exportedAtEpochMillis\":0}",
            )
        }
    }
}
