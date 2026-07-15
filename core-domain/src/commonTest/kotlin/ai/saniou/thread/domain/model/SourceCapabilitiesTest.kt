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
    fun connectorCanDeclareOptionalFeaturesWithoutSourceSpecificDomainTypes() {
        val capabilities = SourceCapabilities(
            supportsTopicCreation = false,
            supportsReplies = true,
            supportsUserContent = true,
            supportsLogin = true,
            commentPageSize = 30,
            hasSubComments = true,
            hasUpvote = true,
            hasJumpPage = true,
        )

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
