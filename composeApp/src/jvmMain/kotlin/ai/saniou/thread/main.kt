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

@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {
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
