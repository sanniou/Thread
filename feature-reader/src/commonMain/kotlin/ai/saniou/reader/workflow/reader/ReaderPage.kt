package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.layout.AdaptiveSidebarScaffold
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.interaction.ThreadShortcut
import ai.saniou.coreui.interaction.threadShortcutHost
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.theme.threadTweenSpec
import ai.saniou.coreui.widgets.AppDrawerItem
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.ArticleItem
import ai.saniou.coreui.widgets.CacheStatusBanner
import ai.saniou.coreui.widgets.CacheStatusTone
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.KeyedLazyListState
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.SectionLabel
import ai.saniou.coreui.widgets.SidebarHeader
import ai.saniou.coreui.widgets.ThreadSearchField
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

class ReaderPage(
    private val initialImportFormat: ReaderSubscriptionFormat? = null,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel<ReaderViewModel>()
        val state by viewModel.state.collectAsState()
        val articles = viewModel.articles.collectAsLazyPagingItems()
        var isAddSheetShown by rememberSaveable { mutableStateOf(false) }
        var editingSource by remember { mutableStateOf<FeedSource?>(null) }
        var isSearchActive by rememberSaveable { mutableStateOf(false) }
        var previewArticle by remember { mutableStateOf<Article?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val searchFocusRequester = remember { FocusRequester() }

        LaunchedEffect(initialImportFormat) {
            initialImportFormat?.let { viewModel.onEvent(ReaderContract.Event.OnShowImport(it)) }
        }

        LaunchedEffect(isSearchActive) {
            if (isSearchActive) searchFocusRequester.requestFocus()
        }

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onEvent(ReaderContract.Event.OnMessageShown)
            }
        }

        state.transferDialog?.let { dialog ->
            ReaderTransferDialog(
                dialog = dialog,
                isWorking = state.isTransferWorking,
                onDismiss = { viewModel.onEvent(ReaderContract.Event.OnDismissTransfer) },
                onImport = { payload ->
                    viewModel.onEvent(ReaderContract.Event.OnImportSubscriptions(payload, dialog.format))
                },
            )
        }

        // Dialog handling
        val isSheetShown = isAddSheetShown || editingSource != null
        if (isSheetShown) {
            val addFeedSourceViewModel =
                remember(editingSource) { AddFeedSourceViewModel(sourceToEdit = editingSource) }
            AddFeedSourceSheet(
                viewModel = addFeedSourceViewModel,
                onDismiss = {
                    isAddSheetShown = false
                    editingSource = null
                },
                onSave = { source ->
                    viewModel.onEvent(ReaderContract.Event.OnSaveSource(source))
                    isAddSheetShown = false
                    editingSource = null
                }
            )
        }

        val windowInfo = LocalThreadWindowInfo.current
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val navigationContent = @Composable {
                FeedSourceList(
                    sources = state.feedSources,
                    articleCounts = state.articleCounts,
                    selectedSourceId = state.selectedFeedSourceId,
                    onSelect = {
                        viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(it))
                        if (!windowInfo.hasPermanentFeatureSidebar) scope.launch { drawerState.close() }
                    },
                    onEdit = { editingSource = it },
                    onDelete = { viewModel.onEvent(ReaderContract.Event.OnDeleteSource(it)) },
                    onRefresh = { viewModel.onEvent(ReaderContract.Event.OnRefreshFeedSource(it)) },
                    onAdd = {
                        editingSource = null
                        isAddSheetShown = true
                        if (!windowInfo.hasPermanentFeatureSidebar) scope.launch { drawerState.close() }
                    },
                    schedulerState = state.scheduler,
                )
        }

        AdaptiveSidebarScaffold(
            modifier = Modifier.threadShortcutHost(
                ThreadShortcut(Key.K) { isSearchActive = true },
                ThreadShortcut(Key.R) { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) },
                ThreadShortcut(Key.Escape, command = false) {
                    when {
                        previewArticle != null -> previewArticle = null
                        isSearchActive -> {
                            isSearchActive = false
                            viewModel.onEvent(ReaderContract.Event.OnSearchQueryChanged(""))
                        }
                    }
                },
            ),
            drawerState = drawerState,
            coroutineScope = scope,
            sidebar = navigationContent,
        ) { showMenu, openSidebar ->
            ReaderScaffold(
                        state = state,
                        articles = articles,
                        isSearchActive = isSearchActive,
                        searchFocusRequester = searchFocusRequester,
                        showMenu = showMenu,
                        onSearchActiveChange = { isSearchActive = it },
                        onSearchQueryChanged = {
                            viewModel.onEvent(
                                ReaderContract.Event.OnSearchQueryChanged(
                                    it
                                )
                            )
                        },
                        onRefreshAll = { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) },
                        onMenuClick = openSidebar,
                        onArticleClick = { article ->
                            viewModel.onEvent(
                                ReaderContract.Event.OnMarkArticleAsRead(
                                    article.id,
                                    true
                                )
                            )
                            if (windowInfo.supportsListDetail) {
                                previewArticle = article
                            } else {
                                navigator.push(ArticleDetailPage(article.id))
                            }
                        },
                        previewArticle = previewArticle,
                        onDismissPreview = { previewArticle = null },
                        onOpenArticleDetail = { navigator.push(ArticleDetailPage(it.id)) },
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) },
                        onListPositionChanged = { contextKey, index, offset ->
                            viewModel.onEvent(
                                ReaderContract.Event.OnListPositionChanged(contextKey, index, offset)
                            )
                        },
                        snackbarHostState = snackbarHostState,
                        onExport = { viewModel.onEvent(ReaderContract.Event.OnExportSubscriptions(it)) },
                        onImport = { viewModel.onEvent(ReaderContract.Event.OnShowImport(it)) },
                        onAddSource = {
                            editingSource = null
                            isAddSheetShown = true
                        },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScaffold(
    state: ReaderContract.State,
    articles: LazyPagingItems<Article>,
    isSearchActive: Boolean,
    searchFocusRequester: FocusRequester,
    showMenu: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onMenuClick: () -> Unit,
    onArticleClick: (Article) -> Unit,
    previewArticle: Article?,
    onDismissPreview: () -> Unit,
    onOpenArticleDetail: (Article) -> Unit,
    onFilterChange: (ArticleFilter) -> Unit,
    onListPositionChanged: (String, Int, Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    onExport: (ReaderSubscriptionFormat) -> Unit,
    onImport: (ReaderSubscriptionFormat) -> Unit,
    onAddSource: () -> Unit,
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                        vertical = 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReaderHeader(
                    state = state,
                    isSearchActive = isSearchActive,
                    searchFocusRequester = searchFocusRequester,
                    showMenuIcon = showMenu,
                    onSearchActiveChange = onSearchActiveChange,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onRefreshAll = onRefreshAll,
                    onMenuClick = onMenuClick,
                    onExport = onExport,
                    onImport = onImport,
                    onAddSource = onAddSource,
                )
                val isRefreshing = state.feedSources.any { it.isRefreshing }
                val cacheTone = when {
                    isRefreshing -> CacheStatusTone.REFRESHING
                    state.refreshFailures.isNotEmpty() -> CacheStatusTone.STALE
                    else -> CacheStatusTone.CACHED
                }
                val cacheTitle = when (cacheTone) {
                    CacheStatusTone.REFRESHING -> "正在同步订阅"
                    CacheStatusTone.STALE -> "本地缓存仍可阅读"
                    else -> "缓存优先 · 本地先展示"
                }
                val cacheDetail = when (cacheTone) {
                    CacheStatusTone.REFRESHING -> "刷新不会清空现有文章列表"
                    CacheStatusTone.STALE -> "${state.refreshFailures.size} 个来源暂未更新，已隔离失败源"
                    else -> "下拉或点击刷新以更新订阅源"
                }
                CacheStatusBanner(
                    title = cacheTitle,
                    tone = cacheTone,
                    detail = cacheDetail,
                    modifier = Modifier.fillMaxWidth(),
                )
                RefreshDiagnosticsBanner(
                    failures = state.refreshFailures,
                    onRetry = onRefreshAll,
                )
                FilterChips(
                    selectedFilter = state.articleFilter,
                    onFilterChange = onFilterChange,
                )
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    PagingStateLayout(
                        items = articles,
                        modifier = Modifier.fillMaxSize().widthIn(max = Dimens.articleListMaxWidth),
                        loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
                        empty = {
                            EmptyState(
                                isSearchActive = state.searchQuery.isNotEmpty(),
                                query = state.searchQuery,
                                hasSources = state.feedSources.isNotEmpty(),
                                onAddSource = onAddSource,
                            )
                        }
                    ) {
                        val listStateKey = buildString {
                            append(state.selectedFeedSourceId ?: "all")
                            append(':')
                            append(state.articleFilter)
                            append(':')
                            append(state.searchQuery.trim())
                        }
                        val restoredAnchor = state.listAnchor?.takeIf { it.contextKey == listStateKey }
                        KeyedLazyListState(
                            stateKey = listStateKey,
                            initialIndex = restoredAnchor?.index ?: 0,
                            initialOffset = restoredAnchor?.offset ?: 0,
                            onPositionChanged = { index, offset ->
                                onListPositionChanged(listStateKey, index, offset)
                            },
                        ) { listState ->
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                                    vertical = 12.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(
                                    count = articles.itemCount,
                                    key = articles.itemKey { it.id },
                                ) { index ->
                                    val article = articles[index]
                                    if (article != null) {
                                        val sourceName = state.feedSources
                                            .find { it.id == article.feedSourceId }
                                            ?.name
                                            ?: "未知来源"
                                        ArticleItem(
                                            article = article,
                                            sourceName = sourceName,
                                            onClick = { onArticleClick(article) },
                                        )
                                    }
                                }

                                item(key = "paging-append") { PagingAppendState(articles) }
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = previewArticle != null && LocalThreadWindowInfo.current.supportsListDetail,
                    enter = fadeIn(animationSpec = threadTweenSpec()) +
                        slideInHorizontally(animationSpec = threadTweenSpec()) { it / 6 },
                    exit = fadeOut(animationSpec = threadTweenSpec()) +
                        slideOutHorizontally(animationSpec = threadTweenSpec()) { it / 6 },
                ) {
                    val article = previewArticle
                    if (article != null) {
                        Row(Modifier.fillMaxHeight()) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            ArticlePreviewPane(
                                article = article,
                                sourceName = state.feedSources.firstOrNull { it.id == article.feedSourceId }?.name
                                    ?: "未知来源",
                                onDismiss = onDismissPreview,
                                onOpenFull = { onOpenArticleDetail(article) },
                                modifier = Modifier.widthIn(min = 440.dp, max = 600.dp).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    isSearchActive: Boolean,
    query: String,
    hasSources: Boolean,
    onAddSource: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ModernEmptyState(
            icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Inbox,
            title = when {
                isSearchActive -> "未找到相关文章"
                !hasSources -> "还没有订阅源"
                else -> "暂无文章"
            },
            description = when {
                isSearchActive -> "没有找到“$query”，换一个关键词继续搜索。"
                !hasSources -> "添加 RSS / Atom 订阅，或导入 OPML 备份，即可开始阅读。"
                else -> "添加订阅源，或稍后刷新内容。"
            },
            action = if (!isSearchActive) {
                {
                    SaniouButton(
                        onClick = onAddSource,
                        text = if (!hasSources) "添加订阅源" else "添加更多源",
                    )
                }
            } else null,
        )
    }
}

@Composable
private fun ArticlePreviewPane(
    article: Article,
    sourceName: String,
    onDismiss: () -> Unit,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    sourceName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenFull) {
                Icon(Icons.Default.OpenInFull, contentDescription = "打开完整阅读页")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭预览")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            article.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                NetworkImage(
                    imageUrl = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.extraLarge),
                )
            }
            Text(
                "QUICK READ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            SelectionContainer {
                Text(
                    article.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                listOfNotNull(article.author, article.publishDate.toString()).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            SelectionContainer {
                RichText(
                    text = article.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ReaderHeader(
    state: ReaderContract.State,
    isSearchActive: Boolean,
    searchFocusRequester: FocusRequester,
    showMenuIcon: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onMenuClick: () -> Unit,
    onExport: (ReaderSubscriptionFormat) -> Unit,
    onImport: (ReaderSubscriptionFormat) -> Unit,
    onAddSource: () -> Unit,
) {
    var transferMenuExpanded by remember { mutableStateOf(false) }
    val selectedSource = state.feedSources.firstOrNull { it.id == state.selectedFeedSourceId }
    val unreadCount = state.articleCounts.values.sumOf { it.second }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ContextHero(
            icon = Icons.Default.AutoStories,
            title = selectedSource?.name ?: "今日阅读",
            subtitle = if (selectedSource == null) {
                "汇集 ${state.feedSources.size} 个订阅源，按你的节奏专注阅读"
            } else {
                "当前订阅源的文章、未读内容与收藏"
            },
            metric = "$unreadCount 未读",
            actions = {
                if (showMenuIcon) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "订阅源")
                    }
                }
                IconButton(onClick = { onSearchActiveChange(!isSearchActive) }) {
                    Icon(
                        if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "关闭搜索" else "搜索",
                    )
                }
                IconButton(onClick = onRefreshAll) {
                    Icon(Icons.Default.Refresh, contentDescription = "全部刷新")
                }
                Box {
                    IconButton(onClick = { transferMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "订阅导入导出")
                    }
                    DropdownMenu(
                        expanded = transferMenuExpanded,
                        onDismissRequest = { transferMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出 JSON") },
                            leadingIcon = { Icon(Icons.Default.Upload, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onExport(ReaderSubscriptionFormat.JSON)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("导出 OPML") },
                            leadingIcon = { Icon(Icons.Default.Upload, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onExport(ReaderSubscriptionFormat.OPML)
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("导入 JSON") },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onImport(ReaderSubscriptionFormat.JSON)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("导入 OPML") },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onImport(ReaderSubscriptionFormat.OPML)
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("添加订阅源") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onAddSource()
                            },
                        )
                    }
                }
            },
        )

        if (isSearchActive) {
            ThreadSearchField(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChanged,
                placeholder = "搜索标题、作者或正文",
                modifier = Modifier.focusRequester(searchFocusRequester),
            )
        }
    }
}

@Composable
private fun FeedSourceList(
    sources: List<FeedSource>,
    articleCounts: Map<String, Pair<Int, Int>>,
    selectedSourceId: String?,
    onSelect: (String?) -> Unit,
    onEdit: (FeedSource) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: (String) -> Unit,
    onAdd: () -> Unit,
    schedulerState: ai.saniou.thread.domain.model.reader.ReaderSchedulerState,
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        SidebarHeader(
            icon = Icons.Default.RssFeed,
            title = "阅读器",
            subtitle = "${sources.size} 个订阅源",
        )
        SectionLabel(
            text = "订阅源",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            item {
                AppDrawerItem(
                    label = "全部文章",
                    icon = Icons.Default.AllInclusive,
                    selected = selectedSourceId == null,
                    onClick = { onSelect(null) },
                    badgeText = articleCounts.values.sumOf { it.second }.takeIf { it > 0 }
                        ?.toString()
                )
            }

            items(sources) { source ->
                val counts = articleCounts[source.id]
                FeedSourceItem(
                    source = source,
                    unreadCount = counts?.second ?: 0,
                    isSelected = selectedSourceId == source.id,
                    onClick = { onSelect(source.id) },
                    onEdit = { onEdit(source) },
                    onDelete = { onDelete(source.id) },
                    onRefresh = { onRefresh(source.id) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = when {
                schedulerState.refreshingSourceIds.isNotEmpty() ->
                    "自动刷新中：${schedulerState.refreshingSourceIds.size} 个源"
                schedulerState.isRunning -> "自动刷新已启用"
                else -> "自动刷新未运行"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )

        AppDrawerItem(
            label = "添加订阅源",
            icon = Icons.Default.Add,
            selected = false,
            onClick = onAdd
        )
    }
}

@Composable
private fun ReaderTransferDialog(
    dialog: ai.saniou.reader.workflow.reader.ReaderTransferDialog,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var payload by remember(dialog) { mutableStateOf(dialog.payload) }
    AdaptiveModal(
        onDismissRequest = { if (!isWorking) onDismiss() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "${if (dialog.isImport) "导入" else "导出"} ${dialog.format.name} 订阅",
                style = MaterialTheme.typography.headlineSmall,
            )
                Text(
                    if (dialog.isImport) "粘贴订阅数据；导入会按 ID 或 URL 合并。"
                    else "复制以下内容并保存；该格式可再次导入 Thread。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { if (dialog.isImport) payload = it },
                    readOnly = !dialog.isImport,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text("数据") },
                )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (dialog.isImport) {
                    SaniouTextButton(onClick = onDismiss, enabled = !isWorking, text = "取消")
                    SaniouButton(
                        onClick = { onImport(payload) },
                        enabled = payload.isNotBlank() && !isWorking,
                        loading = isWorking,
                        text = "导入",
                    )
                } else {
                    SaniouButton(onClick = onDismiss, text = "完成")
                }
            }
        }
    }
}


@Composable
fun FeedSourceItem(
    source: FeedSource,
    unreadCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    AppDrawerItem(
        label = source.name,
        iconUrl = source.iconUrl,
        badgeText = if (unreadCount > 0) unreadCount.toString() else null,
        isLoading = source.isRefreshing,
        selected = isSelected,
        onClick = onClick,
        onLongClick = { showMenu = true },
        trailingContent = {
            // Dropdown Menu
            Box {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("刷新") },
                        onClick = { onRefresh(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { onEdit(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedFilter: ArticleFilter,
    onFilterChange: (ArticleFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(ArticleFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name) },
                leadingIcon = {
                    val icon = when (filter) {
                        ArticleFilter.ALL -> Icons.AutoMirrored.Filled.List
                        ArticleFilter.UNREAD -> Icons.Default.Inbox
                        ArticleFilter.BOOKMARKED -> Icons.Default.Bookmark
                    }
                    if (selectedFilter == filter) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            icon,
                            contentDescription = filter.name,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    }
}
