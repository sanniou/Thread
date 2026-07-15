package ai.saniou.thread.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceCapabilitiesTest {
    @Test
    fun defaultSourceSupportsOnlyBasicPagination() {
        val capabilities = SourceCapabilities.Default

        assertTrue(capabilities.supportsPagination)
        assertTrue(capabilities.supportsChannelCatalog)
        assertTrue(capabilities.supportsFeedAggregation)
        assertFalse(capabilities.hasSubComments)
        assertFalse(capabilities.hasUpvote)
        assertFalse(capabilities.hasPoll)
    }

    @Test
    fun tiebaPresetExposesConnectorFeatures() {
        val capabilities = SourceCapabilities.Tieba

        assertTrue(capabilities.supportsPagination)
        assertFalse(capabilities.supportsTopicCreation)
        assertTrue(capabilities.supportsReplies)
        assertTrue(capabilities.supportsUserContent)
        assertTrue(capabilities.supportsLogin)
        assertTrue(capabilities.commentPageSize == 30)
        assertTrue(capabilities.hasSubComments)
        assertTrue(capabilities.hasUpvote)
        assertTrue(capabilities.hasJumpPage)
    }
}
