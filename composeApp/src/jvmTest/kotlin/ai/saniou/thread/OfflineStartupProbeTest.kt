package ai.saniou.thread

import kotlin.test.Test

class OfflineStartupProbeTest {
    @Test
    fun startupProbeResolvesSeededForumAndReaderContentWithoutNetwork() {
        runDesktopStartupProbe()
    }
}
