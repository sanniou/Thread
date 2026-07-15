package ai.saniou.thread.domain.model.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceDescriptorTest {
    @Test
    fun discourseRequiresValidRuntimeIdentityAndUrl() {
        assertFailsWith<IllegalArgumentException> {
            SourceDescriptor("Bad ID", SourceType.DISCOURSE, "Forum", "https://example.com")
        }
        assertFailsWith<IllegalArgumentException> {
            SourceDescriptor("forum", SourceType.DISCOURSE, "Forum")
        }
        assertFailsWith<IllegalArgumentException> {
            SourceDescriptor("forum", SourceType.DISCOURSE, "Forum", "forum.example.com")
        }
    }

    @Test
    fun connectorKindsRemainOpenForFutureFactories() {
        assertEquals("mastodon", SourceType("mastodon").value)
    }
}
