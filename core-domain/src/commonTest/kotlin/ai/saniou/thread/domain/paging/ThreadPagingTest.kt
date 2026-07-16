package ai.saniou.thread.domain.paging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ThreadPagingTest {
    @Test
    fun createsBoundedCrossPlatformPagingWindow() {
        val config = threadPagingConfig(pageSize = 30)

        assertEquals(30, config.pageSize)
        assertEquals(60, config.initialLoadSize)
        assertEquals(15, config.prefetchDistance)
        assertEquals(300, config.maxSize)
        assertFalse(config.enablePlaceholders)
    }

    @Test
    fun rejectsInvalidWindowProfiles() {
        assertFailsWith<IllegalArgumentException> { threadPagingConfig(pageSize = 0) }
        assertFailsWith<IllegalArgumentException> {
            threadPagingConfig(initialLoadPages = 2, maxCachedPages = 2)
        }
    }
}
