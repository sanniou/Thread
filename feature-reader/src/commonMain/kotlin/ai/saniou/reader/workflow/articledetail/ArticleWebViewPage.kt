package ai.saniou.reader.workflow.articledetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/**
 * 一个用于显示HTML内容的跨平台WebView的占位符。
 * 你需要提供一个真实的实现来替代它。
 */
@Composable
fun WebView(
    modifier: Modifier = Modifier,
    data: String?,
    baseUrl: String?
) {
    // 平台相关的实现会放在这里。
    // 目前，只显示一个占位符。
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("WebView Placeholder\nBaseUrl: $baseUrl\nHasData: ${!data.isNullOrBlank()}")
    }
}


data class ArticleWebViewPage(val articleId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ArticleDetailViewModel = rememberScreenModel(arg = articleId)
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.article?.title ?: "Loading...") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.error != null) {
                    Text(
                        text = "Error: ${state.error?.message}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (state.article != null) {
                    WebView(
                        modifier = Modifier.fillMaxSize(),
                        data = state.article?.rawContent,
                        baseUrl = state.article?.link
                    )
                }
            }
        }
    }
}