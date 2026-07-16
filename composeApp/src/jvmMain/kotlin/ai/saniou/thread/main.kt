package ai.saniou.thread

import ai.saniou.coreui.theme.CupcakeTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.source.SourceCatalog
import org.kodein.di.direct
import org.kodein.di.instance

@OptIn(ExperimentalVoyagerApi::class)
fun main(args: Array<String>) {
    if ("--smoke-check" in args) {
        runDesktopStartupProbe()
        return
    }
    application {
        addTempDirectoryRemovalHook()
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(920.dp, 768.dp),
        )
        CupcakeTheme {
            val attachmentPicker = remember { DesktopAttachmentPicker() }
            Window(
                onCloseRequest = ::exitApplication,
                title = "Thread",
                // Hide default window title
                undecorated = true,
                state = windowState
            ) {
                App(attachmentPicker = attachmentPicker)
            }
        }
    }
}

internal fun runDesktopStartupProbe() {
    val di = createAppDi()
    val catalog = di.direct.instance<SourceCatalog>()
    val sourceIds = catalog.descriptors.value.mapTo(sortedSetOf()) { it.id }
    require(sourceIds.containsAll(listOf("nmb", "tieba", "discourse"))) {
        "Desktop runtime catalog is incomplete: $sourceIds"
    }
    di.direct.instance<ReaderRefreshScheduler>().stop()
    println("Thread Desktop startup probe passed: ${sourceIds.joinToString()}")
}
