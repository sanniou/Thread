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
import ai.saniou.coreui.widgets.ThreadLoadingState
import ai.saniou.coreui.theme.threadAnimateItem
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
import org.jetbrains.compose.resources.stringResource
import thread.feature_reader.generated.resources.reader_export_format_title
import thread.feature_reader.generated.resources.reader_import_format_title
import thread.feature_reader.generated.resources.Res
import thread.feature_reader.generated.resources.s_138be3960f
import thread.feature_reader.generated.resources.s_13f5a71b47
import thread.feature_reader.generated.resources.s_1d3e8e0a39
import thread.feature_reader.generated.resources.s_274a1a8d2e
import thread.feature_reader.generated.resources.s_33246f6a5e
import thread.feature_reader.generated.resources.s_362ae50a1d
import thread.feature_reader.generated.resources.s_3755f56f2f
import thread.feature_reader.generated.resources.s_38108eaa1d
import thread.feature_reader.generated.resources.s_3b9ec1c412
import thread.feature_reader.generated.resources.s_3bbbd45469
import thread.feature_reader.generated.resources.s_452c23cf13
import thread.feature_reader.generated.resources.s_463923ed95
import thread.feature_reader.generated.resources.s_4a8be83972
import thread.feature_reader.generated.resources.s_4d0b4688c7
import thread.feature_reader.generated.resources.s_4fc9662b55
import thread.feature_reader.generated.resources.s_54b8a90b3c
import thread.feature_reader.generated.resources.s_57c031dbd9
import thread.feature_reader.generated.resources.s_60e2bcad85
import thread.feature_reader.generated.resources.s_6f3f39d03c
import thread.feature_reader.generated.resources.s_7829a6c547
import thread.feature_reader.generated.resources.s_7a124b0cee
import thread.feature_reader.generated.resources.s_7ce6f142b0
import thread.feature_reader.generated.resources.s_8837069fef
import thread.feature_reader.generated.resources.s_96cc0e8ed2
import thread.feature_reader.generated.resources.s_a0ecc22def
import thread.feature_reader.generated.resources.s_a701463879
import thread.feature_reader.generated.resources.s_a7f814c0a4
import thread.feature_reader.generated.resources.s_ad3c05564d
import thread.feature_reader.generated.resources.s_b7f317d76b
import thread.feature_reader.generated.resources.s_badc7c8a1d
import thread.feature_reader.generated.resources.s_bb713419e6
import thread.feature_reader.generated.resources.s_bf76308794
import thread.feature_reader.generated.resources.s_c5c986c21d
import thread.feature_reader.generated.resources.s_c96ee4e21a
import thread.feature_reader.generated.resources.s_d2045dd3b8
import thread.feature_reader.generated.resources.s_d506ee7198
import thread.feature_reader.generated.resources.s_dc64bee91b
import thread.feature_reader.generated.resources.s_e1d8a5a688
import thread.feature_reader.generated.resources.s_e25eb142fc
import thread.feature_reader.generated.resources.s_e40a06c88b
import thread.feature_reader.generated.resources.s_e45f66052e
import thread.feature_reader.generated.resources.s_ec41cc2365
import thread.feature_reader.generated.resources.s_f04090805c
import thread.feature_reader.generated.resources.s_f0e21f6e0f
import thread.feature_reader.generated.resources.s_f511238a87
import thread.feature_reader.generated.resources.s_f9f25fb227
import thread.feature_reader.generated.resources.s_fbf6b9bcc8

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
                    CacheStatusTone.REFRESHING -> stringResource(Res.string.s_c5c986c21d)
                    CacheStatusTone.STALE -> stringResource(Res.string.s_e45f66052e)
                    else -> stringResource(Res.string.s_f0e21f6e0f)
                }
                val cacheDetail = when (cacheTone) {
                    CacheStatusTone.REFRESHING -> stringResource(Res.string.s_ad3c05564d)
                    CacheStatusTone.STALE -> stringResource(Res.string.s_274a1a8d2e, state.refreshFailures.size)
                    else -> stringResource(Res.string.s_7829a6c547)
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
                        loading = { ThreadLoadingState(modifier = Modifier.fillMaxSize()) },
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
                                            ?: stringResource(Res.string.s_13f5a71b47)
                                        ArticleItem(
                                            article = article,
                                            sourceName = sourceName,
                                            onClick = { onArticleClick(article) },
                                            modifier = threadAnimateItem(),
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
                                    ?: stringResource(Res.string.s_13f5a71b47),
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
                isSearchActive -> stringResource(Res.string.s_f9f25fb227)
                !hasSources -> stringResource(Res.string.s_f511238a87)
                else -> stringResource(Res.string.s_4a8be83972)
            },
            description = when {
                isSearchActive -> stringResource(Res.string.s_badc7c8a1d, query)
                !hasSources -> stringResource(Res.string.s_b7f317d76b)
                else -> stringResource(Res.string.s_7a124b0cee)
            },
            action = if (!isSearchActive) {
                {
                    SaniouButton(
                        onClick = onAddSource,
                        text = if (!hasSources) stringResource(Res.string.s_8837069fef) else stringResource(Res.string.s_a0ecc22def),
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
                Icon(Icons.Default.OpenInFull, contentDescription = stringResource(Res.string.s_463923ed95))
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.s_bf76308794))
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
            title = selectedSource?.name ?: stringResource(Res.string.s_c96ee4e21a),
            subtitle = if (selectedSource == null) {
                stringResource(Res.string.s_4fc9662b55, state.feedSources.size)
            } else {
                stringResource(Res.string.s_362ae50a1d)
            },
            metric = stringResource(Res.string.s_d506ee7198, unreadCount),
            actions = {
                if (showMenuIcon) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.s_a701463879))
                    }
                }
                IconButton(onClick = { onSearchActiveChange(!isSearchActive) }) {
                    Icon(
                        if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchActive) stringResource(Res.string.s_e40a06c88b) else stringResource(Res.string.s_f04090805c),
                    )
                }
                IconButton(onClick = onRefreshAll) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.s_e1d8a5a688))
                }
                Box {
                    IconButton(onClick = { transferMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.s_57c031dbd9))
                    }
                    DropdownMenu(
                        expanded = transferMenuExpanded,
                        onDismissRequest = { transferMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.s_3b9ec1c412)) },
                            leadingIcon = { Icon(Icons.Default.Upload, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onExport(ReaderSubscriptionFormat.JSON)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.s_452c23cf13)) },
                            leadingIcon = { Icon(Icons.Default.Upload, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onExport(ReaderSubscriptionFormat.OPML)
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.s_d2045dd3b8)) },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onImport(ReaderSubscriptionFormat.JSON)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.s_96cc0e8ed2)) },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = {
                                transferMenuExpanded = false
                                onImport(ReaderSubscriptionFormat.OPML)
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.s_8837069fef)) },
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
                placeholder = stringResource(Res.string.s_dc64bee91b),
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
            title = stringResource(Res.string.s_1d3e8e0a39),
            subtitle = stringResource(Res.string.s_138be3960f, sources.size),
        )
        SectionLabel(
            text = stringResource(Res.string.s_a701463879),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            item {
                AppDrawerItem(
                    label = stringResource(Res.string.s_7ce6f142b0),
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
                    stringResource(Res.string.s_bb713419e6, schedulerState.refreshingSourceIds.size)
                schedulerState.isRunning -> stringResource(Res.string.s_ec41cc2365)
                else -> stringResource(Res.string.s_e25eb142fc)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )

        AppDrawerItem(
            label = stringResource(Res.string.s_8837069fef),
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
                text = stringResource(
                    if (dialog.isImport) Res.string.reader_import_format_title else Res.string.reader_export_format_title,
                    dialog.format.name,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
                Text(
                    if (dialog.isImport) stringResource(Res.string.s_fbf6b9bcc8)
                    else stringResource(Res.string.s_3bbbd45469),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { if (dialog.isImport) payload = it },
                    readOnly = !dialog.isImport,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text(stringResource(Res.string.s_54b8a90b3c)) },
                )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (dialog.isImport) {
                    SaniouTextButton(onClick = onDismiss, enabled = !isWorking, text = stringResource(Res.string.s_4d0b4688c7))
                    SaniouButton(
                        onClick = { onImport(payload) },
                        enabled = payload.isNotBlank() && !isWorking,
                        loading = isWorking,
                        text = stringResource(Res.string.s_60e2bcad85),
                    )
                } else {
                    SaniouButton(onClick = onDismiss, text = stringResource(Res.string.s_33246f6a5e))
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
                        text = { Text(stringResource(Res.string.s_38108eaa1d)) },
                        onClick = { onRefresh(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.s_a7f814c0a4)) },
                        onClick = { onEdit(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.s_3755f56f2f)) },
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
