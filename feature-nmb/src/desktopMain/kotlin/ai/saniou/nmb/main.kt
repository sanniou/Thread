package ai.saniou.nmb

import ai.saniou.nmb.workflow.home.ForumCategoryPage
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(480.dp, 768.dp),
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Thread",
        // Hide default window title
        undecorated = true,
        state = windowState
    ) {
        ForumCategoryPage()
    }
}
