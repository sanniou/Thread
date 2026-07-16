package ai.saniou.thread.data.source.social

import ai.saniou.thread.domain.model.social.SocialCapabilities
import ai.saniou.thread.domain.model.social.SocialIdentity
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialMedia
import ai.saniou.thread.domain.model.social.SocialMediaKind
import ai.saniou.thread.domain.model.social.SocialPost
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FixtureSocialConnectorTest {
    @Test
    fun paginatesLargeMixedFixtureAndAppliesIdempotentInteractions() = runBlocking {
        val fixtures = (0 until 154).map { index ->
            SocialPost(
                id = "post-$index",
                sourceId = "fixture-social",
                author = SocialIdentity("author-${index % 12}", "Author ${index % 12}"),
                body = "Post body $index",
                createdAtEpochMillis = index.toLong(),
                contentWarning = "Spoiler".takeIf { index % 7 == 0 },
                media = if (index % 4 == 0) listOf(
                    SocialMedia("media-$index", SocialMediaKind.IMAGE, "https://example.com/$index.jpg"),
                ) else emptyList(),
                interactionCounts = mapOf(SocialInteraction.LIKE to index.toLong()),
                permittedInteractions = setOf(SocialInteraction.LIKE, SocialInteraction.BOOKMARK),
            )
        }
        val connector = FixtureSocialConnector(
            sourceId = "fixture-social",
            fixtures = fixtures,
            pageSize = 40,
            capabilities = SocialCapabilities(
                interactions = setOf(SocialInteraction.LIKE, SocialInteraction.BOOKMARK),
            ),
        )

        val first = connector.timeline().getOrThrow()
        assertEquals(40, first.items.size)
        assertNull(first.newerCursor)
        assertNotNull(first.olderCursor)
        val second = connector.timeline(first.olderCursor).getOrThrow()
        assertEquals("post-113", second.items.first().id)
        assertNotNull(second.newerCursor)

        val liked = connector.interact("post-153", SocialInteraction.LIKE, true).getOrThrow()
        assertEquals(154L, liked.interactionCounts[SocialInteraction.LIKE])
        val repeated = connector.interact("post-153", SocialInteraction.LIKE, true).getOrThrow()
        assertEquals(154L, repeated.interactionCounts[SocialInteraction.LIKE])
        val unliked = connector.interact("post-153", SocialInteraction.LIKE, false).getOrThrow()
        assertEquals(153L, unliked.interactionCounts[SocialInteraction.LIKE])

        var cursor = first.olderCursor
        var total = first.items.size
        while (cursor != null) {
            val page = connector.timeline(cursor).getOrThrow()
            total += page.items.size
            cursor = page.olderCursor
        }
        assertEquals(154, total)
    }
}
