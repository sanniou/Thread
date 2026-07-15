package ai.saniou.thread.feature.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import ai.saniou.thread.network.NetworkConstants
import com.multiplatform.webview.cookie.Cookie
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import kotlinx.coroutines.delay
import platform.WebKit.WKWebView

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
        onCreated = { webView: WKWebView ->
            webView.customUserAgent = NetworkConstants.USER_AGENT
            webView.configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        },
        onDispose = { _: WKWebView -> }
    )
}

private fun formatCookies(cookies: List<Cookie>): String {
    return cookies.joinToString("; ") { "${it.name}=${it.value}" }
}
