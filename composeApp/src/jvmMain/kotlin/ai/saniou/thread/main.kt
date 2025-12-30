package ai.saniou.thread

import ai.saniou.coreui.theme.CupcakeTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalVoyagerApi::class)
fun main() = application {
    addTempDirectoryRemovalHook()
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(920.dp, 768.dp),
    )
    CupcakeTheme {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Thread",
            // Hide default window title
            undecorated = true,
            state = windowState
        ) {
//            KcefWrapper {
                App()
//            }
        }
    }
}

@Composable
fun KcefWrapper(content: @Composable () -> Unit) {
    var restartRequired by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(0F) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            KCEF.init(builder = {
                installDir(File("kcef-bundle"))
                progress {
                    onDownloading {
                        downloading = it
                    }
                    onInitialized {
                        initialized = true
                    }
                }
                download {
                    github {
                        release("jbr-release-17.0.12b1207.37")
                    }
                }

                settings {
                    cachePath = File("cache").absolutePath
                }
            }, onError = {
                it?.printStackTrace()
            }, onRestartRequired = {
                restartRequired = true
            })
        }
    }

    if (restartRequired) {
        RestartRequiredScreen()
    } else {
        if (initialized) {
            content()
        } else {
            LoadingScreen(downloadProgress = downloading)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            KCEF.disposeBlocking()
        }
    }
}


@Composable
fun LoadingScreen(downloadProgress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.Black,
                strokeWidth = 2.dp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Compose WebView Multiplatform",
                fontSize = 18.sp,
                color = Color.Black,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Initializing...",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = downloadProgress / 100f,
                modifier =
                    Modifier
                        .size(
                            width = 200.dp,
                            height = 4.dp,
                        ),
                color = Color.Black,
                backgroundColor = Color.Black.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${downloadProgress.toInt()}%",
                fontSize = 12.sp,
                color = Color.Black,
            )
        }
    }
}

@Composable
fun RestartRequiredScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Restart Required",
                fontSize = 18.sp,
                color = Color.Black,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please restart the application to continue.",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f),
            )
        }
    }
}
