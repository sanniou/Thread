package ai.saniou.forum.workflow.login

import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.workflow.login.TiebaLoginContract.Effect
import ai.saniou.forum.workflow.login.TiebaLoginContract.Event
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.multiplatform.webview.cookie.Cookie
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.DI

data class TiebaLoginScreen(
    val di: DI = nmbdi,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: TiebaLoginViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        val webViewState = rememberWebViewState(url = state.loginUrl)
        val webViewNavigator = rememberWebViewNavigator()

        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is Effect.NavigateBack -> navigator.pop()
                    is Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        // Cookie Monitor
        LaunchedEffect(webViewState.loadingState) {
            if (webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
                // Get Cookies
                val cookies = webViewState.cookieManager.getCookies(state.loginUrl)
                checkCookies(cookies, viewModel)
            }
        }

        Scaffold(
            topBar = {
                SaniouTopAppBar(
                    title = "登录贴吧",
                    onNavigationClick = { navigator.pop() }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                WebView(
                    state = webViewState,
                    navigator = webViewNavigator,
                    modifier = Modifier.fillMaxSize()
                )

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    private fun checkCookies(cookies: List<Cookie>, viewModel: TiebaLoginViewModel) {
        var bduss: String? = null
        var stoken: String? = null
        // Placeholder for user extraction logic if available in cookies or URL
        // In reality, might need to extract from page content or specific cookie fields
        // Assuming simple cookie extraction for now
        
        for (cookie in cookies) {
            if (cookie.name == "BDUSS") {
                bduss = cookie.value
            }
            if (cookie.name == "STOKEN") {
                stoken = cookie.value
            }
        }

        if (bduss != null && stoken != null) {
            // Found credentials, trigger interception
            // Note: We might miss uid/name/portrait here. 
            // The ViewModel should ideally fetch them if missing, 
            // or LoginTiebaUseCase should handle partial info.
            // For this implementation, we assume we need to trigger a fetch.
            
            // To simplify for this step, pass empty strings for unknown fields 
            // and let the UseCase or ViewModel handle it (as planned in next steps).
            viewModel.handleEvent(
                Event.CredentialsIntercepted(
                    bduss = bduss,
                    stoken = stoken,
                    uid = "", // Unknown
                    name = "", // Unknown
                    portrait = "", // Unknown
                    tbs = "" // Unknown
                )
            )
        }
    }
}