package ai.saniou.reader.workflow.articledetail

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.thread.domain.model.reader.Article
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class ArticleDetailPage(val articleId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ArticleDetailViewModel = rememberScreenModel(arg = articleId)
        val state by viewModel.state.collectAsState()
        val uriHandler = LocalUriHandler.current
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ArticleDetailTopAppBar(
                    state = state,
                    scrollBehavior = scrollBehavior,
                    onBack = { navigator.pop() },
                    onToggleBookmark = { viewModel.onEvent(ArticleDetailContract.Event.OnToggleBookmark) },
                    onShare = { /* TODO: Implement share */ },
                    onOpenInBrowser = { state.article?.link?.let { uriHandler.openUri(it) } },
                    onShowWebView = { state.article?.id?.let { navigator.push(ArticleWebViewPage(it)) } },
                    onToggleMenu = { viewModel.onEvent(ArticleDetailContract.Event.OnToggleMenu(it)) },
                    onFontSizeChange = { viewModel.onEvent(ArticleDetailContract.Event.OnChangeFontSize(it)) }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.error != null -> {
                        ErrorState(
                            error = state.error,
                            onRetry = { viewModel.onEvent(ArticleDetailContract.Event.OnRetry) }
                        )
                    }
                    state.article != null -> {
                        ArticleContent(
                            article = state.article!!,
                            fontSizeScale = state.fontSizeScale
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(error: Throwable?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error?.message ?: "未知错误",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("重试")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleDetailTopAppBar(
    state: ArticleDetailContract.State,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShowWebView: () -> Unit,
    onToggleMenu: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
    SaniouTopAppBar(
        title = state.feedSourceName ?: "文章详情",
        onNavigationClick = onBack,
        actions = {
            state.article?.let { article ->
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (article.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "收藏",
                        tint = if (article.isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "分享")
                }
                Box {
                    IconButton(onClick = { onToggleMenu(true) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                    }
                    DropdownMenu(
                        expanded = state.isMenuExpanded,
                        onDismissRequest = { onToggleMenu(false) }
                    ) {
                        // 字体调整
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatSize, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("字体大小")
                                }
                            },
                            onClick = { /* Keep menu open */ },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onFontSizeChange((state.fontSizeScale - 0.1f).coerceAtLeast(0.8f)) }
                                    ) { Text("-") }
                                    Text("${(state.fontSizeScale * 100).toInt()}%")
                                    IconButton(
                                        onClick = { onFontSizeChange((state.fontSizeScale + 0.1f).coerceAtMost(1.5f)) }
                                    ) { Text("+") }
                                }
                            }
                        )
                        HorizontalDivider()

                        if (!article.rawContent.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("网页视图") },
                                onClick = {
                                    onShowWebView()
                                    onToggleMenu(false)
                                },
                                leadingIcon = { Icon(Icons.Default.Web, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("浏览器打开") },
                            onClick = {
                                onOpenInBrowser()
                                onToggleMenu(false)
                            },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ArticleContent(article: Article, fontSizeScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp) // 增加边距
    ) {
        // 标题区
        SelectionContainer {
            Column {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 元数据区 (作者、时间、来源)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (article.author != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(article.author!!) },
                            leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) }
                        )
                    }
                    Text(
                        text = article.publishDate.toRelativeTimeString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        // 正文内容
        SelectionContainer {
            // 注意：这里假设 RichText 支持 modifier 或 style 调整。
            // 如果 RichText 是第三方库且不完全支持 textStyle scale，可能需要调整 RichText 实现。
            // 这里我们传递放大后的 style。
            val baseStyle = MaterialTheme.typography.bodyLarge
            val scaledStyle = baseStyle.copy(fontSize = baseStyle.fontSize * fontSizeScale)

            RichText(
                text = article.content,
                style = scaledStyle,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 底部留白
        Spacer(modifier = Modifier.height(64.dp))
    }
}
