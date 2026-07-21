package ai.saniou.reader.workflow.articledetail

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.platform.LocalShareService
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.layout.ReadingCanvas
import ai.saniou.coreui.state.toAppError
import ai.saniou.coreui.theme.LocalThreadUiPreferences
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ThreadErrorState
import ai.saniou.coreui.widgets.RelatedContentSection
import ai.saniou.coreui.widgets.ActionItem
import ai.saniou.coreui.widgets.UnifiedActionBar
import ai.saniou.coreui.composition.LocalContentLinkHandler
import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.RelatedContent
import ai.saniou.thread.domain.model.content.toThreadUrl
import ai.saniou.thread.domain.repository.ContentGraphRepository
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.workspace.RestorableContentKind
import ai.saniou.thread.domain.model.workspace.RestorableContentReference
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems

data class ArticleDetailPage(val articleId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val updateWorkspaceSession = di.direct.instance<UpdateWorkspaceSessionUseCase>()
        val contentGraphRepository = di.direct.instance<ContentGraphRepository>()
        val viewModel: ArticleDetailViewModel = rememberScreenModel(arg = articleId)
        val state by viewModel.state.collectAsState()
        val uriHandler = LocalUriHandler.current
        val clipboard = rememberThreadClipboard()
        val shareService = LocalShareService.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val rootLinkHandler = LocalContentLinkHandler.current
        val graphReference = remember(articleId) {
            ContentReference(ContentReferenceKind.ARTICLE, articleId)
        }
        val relatedFlow = remember(graphReference) { contentGraphRepository.getRelated(graphReference) }
        val related = relatedFlow.collectAsLazyPagingItems()
        val contentReference = remember(articleId) {
            RestorableContentReference(
                kind = RestorableContentKind.ARTICLE,
                id = articleId,
                workspace = ai.saniou.thread.domain.model.workspace.WorkspaceDestination.READER,
            )
        }
        LaunchedEffect(contentReference) {
            updateWorkspaceSession { current ->
                current.copy(
                    lastContent = contentReference.copy(workspace = current.destination),
                    updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
        LaunchedEffect(state.article?.id) {
            if (state.article != null) contentGraphRepository.rebuild(graphReference)
        }
        fun closeDetail() {
            scope.launch {
                updateWorkspaceSession { current ->
                    val lastContent = current.lastContent
                    if (lastContent?.kind == contentReference.kind &&
                        lastContent.id == contentReference.id
                    ) {
                        current.copy(
                            lastContent = null,
                            updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                        )
                    } else {
                        current
                    }
                }
                navigator.pop()
            }
        }
        ThreadDetailScaffold(
            title = state.feedSourceName ?: "文章详情",
            eyebrow = "阅读",
            subtitle = state.article?.title,
            onBack = ::closeDetail,
            actions = {
                ArticleDetailActions(
                    state = state,
                    onToggleBookmark = { viewModel.onEvent(ArticleDetailContract.Event.OnToggleBookmark) },
                    onShare = {
                        state.article?.let { article ->
                            val text = "${article.title}\n${article.link}"
                            val shared = shareService?.shareText(text, article.title) == true
                            if (!shared) clipboard.copyText(text)
                            scope.launch {
                                snackbar.showSnackbar(if (shared) "已通过系统分享" else "标题和链接已复制")
                            }
                        }
                    },
                    onOpenInBrowser = { state.article?.link?.let { uriHandler.openUri(it) } },
                    onShowWebView = { state.article?.id?.let { navigator.push(ArticleWebViewPage(it)) } },
                    onToggleMenu = { viewModel.onEvent(ArticleDetailContract.Event.OnToggleMenu(it)) },
                    onFontSizeChange = { viewModel.onEvent(ArticleDetailContract.Event.OnChangeFontSize(it)) },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                val article = state.article
                UnifiedActionBar(
                    visible = article != null,
                    actions = listOf(
                        ActionItem(
                            label = if (article?.isBookmarked == true) "已收藏" else "收藏",
                            icon = if (article?.isBookmarked == true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            emphasized = article?.isBookmarked == true,
                            onClick = { viewModel.onEvent(ArticleDetailContract.Event.OnToggleBookmark) },
                        ),
                        ActionItem(
                            label = "分享",
                            icon = Icons.Default.Share,
                            onClick = {
                                article?.let {
                                    val text = "${it.title}\n${it.link}"
                                    val shared = shareService?.shareText(text, it.title) == true
                                    if (!shared) clipboard.copyText(text)
                                    scope.launch {
                                        snackbar.showSnackbar(if (shared) "已通过系统分享" else "标题和链接已复制")
                                    }
                                }
                            },
                        ),
                        ActionItem(
                            label = "浏览器",
                            icon = Icons.Default.Public,
                            onClick = { article?.link?.let { uriHandler.openUri(it) } },
                        ),
                    ),
                )
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.error != null -> {
                        ThreadErrorState(
                            error = state.error!!.toAppError(),
                            onRetry = { viewModel.onEvent(ArticleDetailContract.Event.OnRetry) },
                        )
                    }
                    state.article != null -> {
                        ArticleContent(
                            article = state.article!!,
                            fontSizeScale = state.fontSizeScale,
                            related = related,
                            onOpenRelated = { rootLinkHandler?.invoke(it.reference.toThreadUrl()) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleDetailActions(
    state: ArticleDetailContract.State,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShowWebView: () -> Unit,
    onToggleMenu: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
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
}

@Composable
private fun ArticleContent(
    article: Article,
    fontSizeScale: Float,
    related: LazyPagingItems<RelatedContent>,
    onOpenRelated: (RelatedContent) -> Unit,
) {
    val windowInfo = LocalThreadWindowInfo.current
    val uiPreferences = LocalThreadUiPreferences.current
    ReadingCanvas {
        Column(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = uiPreferences.readerWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = windowInfo.pageHorizontalPadding, vertical = Dimens.padding_extra_large)
        ) {
            article.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                NetworkImage(
                    imageUrl = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 360.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
                Spacer(Modifier.height(Dimens.padding_extra_large))
            }

            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)) {
                    Text(
                        text = "阅读 · ${article.publishDate.toRelativeTimeString()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        article.author?.takeIf { it.isNotBlank() }?.let { author ->
                            AssistChip(
                                onClick = {},
                                label = { Text(author) },
                                leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) },
                            )
                        }
                        Text(
                            text = if (article.isBookmarked) "已收藏到稍后阅读" else "沉浸阅读模式",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.padding_large))

            SelectionContainer {
                val baseStyle = MaterialTheme.typography.bodyLarge
                val scaledStyle = baseStyle.copy(
                    fontSize = baseStyle.fontSize * fontSizeScale,
                    lineHeight = baseStyle.fontSize * fontSizeScale * uiPreferences.readerLineHeightMultiplier,
                )
                RichText(
                    text = article.content,
                    style = scaledStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.padding_large))
            RelatedContentSection(items = related, onOpen = onOpenRelated)
            Spacer(modifier = Modifier.height(Dimens.size_120))
        }
    }
}
