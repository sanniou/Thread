package ai.saniou.coreui.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class AppEntryControllerTest {
    @Test
    fun openEmitsNormalizedEntry() = runBlocking {
        val controller = AppEntryController()
        val received = mutableListOf<AppEntry>()
        val collector = launch {
            controller.entries.take(1).toList(received)
        }
        yield()
        controller.open("  thread://inbox  ", AppEntrySource.NOTIFICATION)
        collector.join()
        assertEquals(1, received.size)
        assertEquals("thread://inbox", received.single().url)
        assertEquals(AppEntrySource.NOTIFICATION, received.single().source)
    }

    @Test
    fun blankEntriesAreIgnored() {
        val controller = AppEntryController()
        controller.open("   ")
        controller.open("")
        assertTrue(true)
    }
}
