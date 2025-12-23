package ai.saniou.thread

import ai.saniou.coreui.theme.CupcakeTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi

@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(480.dp, 768.dp),
    )
    CupcakeTheme {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Thread",
            // Hide default window title
            undecorated = true,
            state = windowState
        ) {
            App()
        }
    }
}

