package ai.saniou.reader.workflow.articledetail

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.domain.model.Article
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class ArticleDetailPage(val articleId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ArticleDetailViewModel = rememberScreenModel(arg = articleId)
        val state by viewModel.state.collectAsState()
        val uriHandler = LocalUriHandler.current

        Scaffold(
            topBar = {
                ArticleDetailTopAppBar(
                    state = state,
                    onBack = { navigator.pop() },
                    onToggleBookmark = { /* TODO */ },
                    onShare = { /* TODO */ },
                    onOpenInBrowser = { state.article?.link?.let { uriHandler.openUri(it) } },
                    onShowWebView = { state.article?.id?.let { navigator.push(ArticleWebViewPage(it)) } }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when {
                    state.isLoading -> {
                        // TODO: Replace with Shimmer effect
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null -> {
                        // TODO: Replace with a more friendly error component with retry button
                        Text(
                            text = "Error: ${state.error?.message}",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.article != null -> {
                        ArticleContent(article = state.article!!)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleDetailTopAppBar(
    state: ArticleDetailContract.State,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShowWebView: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = state.article?.title ?: "正在加载...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            state.article?.let { article ->
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "收藏"
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "分享")
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!article.rawContent.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("网页视图") },
                                onClick = {
                                    onShowWebView()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Web, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("浏览器打开") },
                            onClick = {
                                onOpenInBrowser()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
                        )
                        // TODO: Add font size and theme options here
                    }
                }
            }
        }
    )
}

@Composable
private fun ArticleContent(article: Article) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(article.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        ArticleMetadata(article)
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        RichText(
            text = article.content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ArticleMetadata(article: Article) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        article.author?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (article.author != null) {
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = article.publishDate.toRelativeTimeString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
