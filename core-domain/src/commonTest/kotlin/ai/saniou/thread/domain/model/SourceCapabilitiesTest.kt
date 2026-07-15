package ai.saniou.thread.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceCapabilitiesTest {
    @Test
    fun defaultSourceSupportsOnlyBasicPagination() {
        val capabilities = SourceCapabilities.Default

        assertTrue(capabilities.supportsPagination)
        assertFalse(capabilities.hasSubComments)
        assertFalse(capabilities.hasUpvote)
        assertFalse(capabilities.hasPoll)
    }

    @Test
    fun tiebaPresetExposesConnectorFeatures() {
        val capabilities = SourceCapabilities.Tieba

        assertTrue(capabilities.supportsPagination)
        assertTrue(capabilities.hasSubComments)
        assertTrue(capabilities.hasUpvote)
        assertTrue(capabilities.hasJumpPage)
    }
}
