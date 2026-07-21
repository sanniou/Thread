package ai.saniou.reader.workflow.articledetail

import ai.saniou.coreui.state.toAppError
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ThreadErrorState
import ai.saniou.coreui.widgets.ThreadLoadingState
import ai.saniou.thread.domain.model.reader.Article
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

data class ArticleWebViewPage(val articleId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ArticleDetailViewModel = rememberScreenModel(arg = articleId)
        val state by viewModel.state.collectAsState()

        ThreadDetailScaffold(
            title = state.feedSourceName ?: "网页视图",
            eyebrow = "原文",
            subtitle = state.article?.title,
            onBack = navigator::pop,
            actions = {
                IconButton(onClick = { viewModel.onEvent(ArticleDetailContract.Event.OnRetry) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新加载文章")
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    state.isLoading -> ThreadLoadingState()
                    state.error != null -> ThreadErrorState(
                        error = state.error!!.toAppError(),
                        onRetry = { viewModel.onEvent(ArticleDetailContract.Event.OnRetry) },
                    )
                    state.article != null -> ArticleWebContent(state.article!!)
                }
            }
        }
    }
}

@Composable
private fun ArticleWebContent(article: Article) {
    val navigator = rememberWebViewNavigator()
    val rawContent = article.rawContent
    val state = if (rawContent.isNullOrBlank()) {
        rememberWebViewState(article.link)
    } else {
        rememberWebViewStateWithHTMLData(
            data = rawContent,
            baseUrl = article.link,
        )
    }
    WebView(
        state = state,
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
    )
}
