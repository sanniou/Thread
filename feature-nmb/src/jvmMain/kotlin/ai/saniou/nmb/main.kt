package ai.saniou.nmb

import ai.saniou.coreui.theme.CupcakeTheme
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.home.HomeScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.ScaleTransition
import org.kodein.di.compose.withDI

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
            withDI(nmbdi) {
                Navigator(
                    screen = HomeScreen(),
                    disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false),
                )
            }
        }
    }
}
