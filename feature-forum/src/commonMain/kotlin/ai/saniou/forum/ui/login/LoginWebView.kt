package ai.saniou.forum.ui.login

import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.eyebrow_secure_login
import thread.feature_forum.generated.resources.s_e16cc8bcd0

@Composable
fun LoginWebView(
    strategy: LoginStrategy.WebView,
    onDismissRequest: () -> Unit,
    onCookieCaptured: (Map<String, String>) -> Unit,
) {
    val state = rememberWebViewState(url = strategy.url)
    val navigator = rememberWebViewNavigator()
    var captured by remember { mutableStateOf(false) }

    LaunchedEffect(state.loadingState) {
        if (!captured && state.loadingState is LoadingState.Finished) {
            val cookies = state.cookieManager.getCookies(strategy.cookieDomain ?: strategy.url)
            val values = cookies.associate { it.name to it.value }
            if (strategy.targetCookieKeys.all(values::containsKey)) {
                captured = true
                onCookieCaptured(
                    values + ("cookie" to cookies.joinToString("; ") { "${it.name}=${it.value}" })
                )
            }
        }
    }

    ThreadDetailScaffold(
        title = stringResource(Res.string.s_e16cc8bcd0),
        eyebrow = stringResource(Res.string.eyebrow_secure_login),
        subtitle = strategy.url,
        onBack = onDismissRequest,
    ) { padding ->
        WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
