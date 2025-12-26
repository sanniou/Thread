package ai.saniou.thread.feature.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.multiplatform.webview.cookie.Cookie
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import kotlinx.coroutines.delay

@Composable
actual fun ChallengeWebView(
    url: String,
    modifier: Modifier,
    onChallengeSuccess: (String) -> Unit
) {
    val navigator = rememberWebViewNavigator()
    val state = rememberWebViewState(url)

    LaunchedEffect(state.loadingState) {
        while (true) {
            val cookies = state.cookieManager.getCookies(url)
            val cfClearance = cookies.find { it.name == "cf_clearance" }
            if (cfClearance != null) {
                onChallengeSuccess(formatCookies(cookies))
                break
            }
            delay(1000)
        }
    }

    WebView(
        state = state,
        navigator = navigator,
        modifier = modifier,
        onCreated = {
            it.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            it.settings.javaScriptEnabled = true
            it.settings.domStorageEnabled = true
        },
        onDispose = {}
    )
}

private fun formatCookies(cookies: List<Cookie>): String {
    return cookies.joinToString("; ") { "${it.name}=${it.value}" }
}