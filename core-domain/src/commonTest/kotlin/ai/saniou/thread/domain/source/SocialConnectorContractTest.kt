package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.social.CursorDirection
import ai.saniou.thread.domain.model.social.SocialCapabilities
import ai.saniou.thread.domain.model.social.SocialCursor
import ai.saniou.thread.domain.model.social.SocialIdentity
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialTimelinePage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SocialConnectorContractTest {
    @Test
    fun opaqueCursorIdentityAndInteractionsRemainPlatformNeutral() = runBlocking {
        val connector = FakeSocialConnector()
        val cursor = SocialCursor("opaque:next:/?page=2", CursorDirection.OLDER)
        val page = connector.timeline(cursor).getOrThrow()

        assertEquals(cursor, connector.receivedCursor)
        assertEquals("author@example", page.items.single().author.handle)
        assertTrue(SocialInteraction.BOOKMARK in connector.capabilities.interactions)
        assertEquals("opaque:newer", page.newerCursor?.value)
        assertEquals(6, connector.capabilities.maxMediaItems)
    }
}

private class FakeSocialConnector : SocialConnector {
    override val sourceId = "social-test"
    override val capabilities = SocialCapabilities(
        interactions = setOf(SocialInteraction.REPLY, SocialInteraction.BOOKMARK),
        maxMediaItems = 6,
    )
    var receivedCursor: SocialCursor? = null

    override suspend fun timeline(cursor: SocialCursor?): Result<SocialTimelinePage> {
        receivedCursor = cursor
        return Result.success(
            SocialTimelinePage(
                items = listOf(
                    SocialPost(
                        id = "post-1",
                        sourceId = sourceId,
                        author = SocialIdentity("author-1", "Author", "author@example"),
                        body = "Platform-neutral post",
                        createdAtEpochMillis = 1,
                        permittedInteractions = capabilities.interactions,
                    )
                ),
                newerCursor = SocialCursor("opaque:newer", CursorDirection.NEWER),
            )
        )
    }

    override suspend fun interact(
        postId: String,
        interaction: SocialInteraction,
        enabled: Boolean,
    ): Result<SocialPost> = Result.failure(UnsupportedOperationException())
}
