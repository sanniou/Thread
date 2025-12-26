package ai.saniou.thread.feature.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    onChallengeSuccess: (String) -> Unit,
) {
    val navigator = rememberWebViewNavigator()
    val state = rememberWebViewState(url)
    DisposableEffect(Unit) {
        state.webSettings.apply {
            isJavaScriptEnabled = true
            androidWebSettings.apply {
                isAlgorithmicDarkeningAllowed = true
                safeBrowsingEnabled = true
            }
        }
        onDispose { }
    }
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
        onCreated = { it ->
            // jvm 平台可能不支持 userAgentString，需要检查 API。
            // 假设库在各平台API一致，但 Desktop 实现基于 CEF 或 JCEF，可能有所不同。
            // 这里的 API 应该是公共 API。
            // it.settings.javaScriptEnabled = true
            // it.settings.domStorageEnabled = true // Desktop 可能没有这个属性
        },
        onDispose = {}
    )
}

private fun formatCookies(cookies: List<Cookie>): String {
    return cookies.joinToString("; ") { "${it.name}=${it.value}" }
}
