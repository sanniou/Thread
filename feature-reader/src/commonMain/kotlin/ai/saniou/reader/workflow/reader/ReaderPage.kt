package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.composition.LocalAppDrawer
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AppDrawerItem
import ai.saniou.coreui.widgets.ArticleItem
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
import ai.saniou.coreui.widgets.SectionLabel
import ai.saniou.coreui.widgets.SidebarHeader
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState.Loading
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

class ReaderPage : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel<ReaderViewModel>()
        val state by viewModel.state.collectAsState()
        val articles = viewModel.articles.collectAsLazyPagingItems()
        var isAddSheetShown by remember { mutableStateOf(false) }
        var editingSource by remember { mutableStateOf<FeedSource?>(null) }
        var isSearchActive by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

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

        // Adaptive Layout Logic
        BoxWithConstraints {
            val isMobile = maxWidth < Dimens.MobileWidth
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            val onMenuClick = {
                scope.launch { drawerState.open() }
            }

            val navigationContent = @Composable {
                FeedSourceList(
                    sources = state.feedSources,
                    articleCounts = state.articleCounts,
                    selectedSourceId = state.selectedFeedSourceId,
                    onSelect = {
                        viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(it))
                        if (isMobile) scope.launch { drawerState.close() }
                    },
                    onEdit = { editingSource = it },
                    onDelete = { viewModel.onEvent(ReaderContract.Event.OnDeleteSource(it)) },
                    onRefresh = { viewModel.onEvent(ReaderContract.Event.OnRefreshFeedSource(it)) },
                    onAdd = {
                        editingSource = null
                        isAddSheetShown = true
                        if (isMobile) scope.launch { drawerState.close() }
                    },
                    schedulerState = state.scheduler,
                )
            }

            if (isMobile) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            navigationContent()
                        }
                    }
                ) {
                    ReaderScaffold(
                        state = state,
                        articles = articles,
                        isSearchActive = isSearchActive,
                        isMobile = true,
                        onSearchActiveChange = { isSearchActive = it },
                        onSearchQueryChanged = {
                            viewModel.onEvent(
                                ReaderContract.Event.OnSearchQueryChanged(
                                    it
                                )
                            )
                        },
                        onRefreshAll = { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onArticleClick = { article ->
                            viewModel.onEvent(
                                ReaderContract.Event.OnMarkArticleAsRead(
                                    article.id,
                                    true
                                )
                            )
                            navigator.push(ArticleDetailPage(article.id))
                        },
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) },
                        snackbarHostState = snackbarHostState,
                        onExport = { viewModel.onEvent(ReaderContract.Event.OnExportSubscriptions(it)) },
                        onImport = { viewModel.onEvent(ReaderContract.Event.OnShowImport(it)) },
                    )
                }
            } else {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(
                            modifier = Modifier.width(Dimens.sidebarWidth),
                            drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            navigationContent()
                        }
                    }
                ) {
                    ReaderScaffold(
                        state = state,
                        articles = articles,
                        isSearchActive = isSearchActive,
                        isMobile = false,
                        onSearchActiveChange = { isSearchActive = it },
                        onSearchQueryChanged = {
                            viewModel.onEvent(
                                ReaderContract.Event.OnSearchQueryChanged(
                                    it
                                )
                            )
                        },
                        onRefreshAll = { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) },
                        onMenuClick = {}, // No menu button on desktop
                        onArticleClick = { article ->
                            viewModel.onEvent(
                                ReaderContract.Event.OnMarkArticleAsRead(
                                    article.id,
                                    true
                                )
                            )
                            navigator.push(ArticleDetailPage(article.id))
                        },
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) },
                        snackbarHostState = snackbarHostState,
                        onExport = { viewModel.onEvent(ReaderContract.Event.OnExportSubscriptions(it)) },
                        onImport = { viewModel.onEvent(ReaderContract.Event.OnShowImport(it)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScaffold(
    state: ReaderContract.State,
    articles: LazyPagingItems<Article>,
    isSearchActive: Boolean,
    isMobile: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onMenuClick: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onFilterChange: (ArticleFilter) -> Unit,
    snackbarHostState: SnackbarHostState,
    onExport: (ReaderSubscriptionFormat) -> Unit,
    onImport: (ReaderSubscriptionFormat) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ReaderTopAppBar(
                state = state,
                isSearchActive = isSearchActive,
                showMenuIcon = isMobile,
                onSearchActiveChange = onSearchActiveChange,
                onSearchQueryChanged = onSearchQueryChanged,
                onRefreshAll = onRefreshAll,
                onMenuClick = onMenuClick,
                onExport = onExport,
                onImport = onImport,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)) {
                    RefreshDiagnosticsBanner(
                        failures = state.refreshFailures,
                        modifier = Modifier.padding(horizontal = Dimens.page_horizontal, vertical = 8.dp),
                    )
                    FilterChips(
                        selectedFilter = state.articleFilter,
                        onFilterChange = onFilterChange,
                        modifier = Modifier.padding(horizontal = Dimens.page_horizontal),
                    )
                }
            }
            PagingStateLayout(
                items = articles,
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                    .align(Alignment.CenterHorizontally),
                loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
                empty = {
                    EmptyState(
                        isSearchActive = state.searchQuery.isNotEmpty(),
                        query = state.searchQuery
                    )
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = Dimens.page_horizontal,
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(articles.itemCount) { index ->
                        val article = articles[index]
                        if (article != null) {
                            val sourceName =
                                state.feedSources.find { it.id == article.feedSourceId }?.name
                                    ?: "未知来源"
                            ArticleItem(
                                article = article,
                                sourceName = sourceName,
                                onClick = { onArticleClick(article) }
                            )
                        }
                    }

                    if (articles.loadState.append is Loading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isSearchActive: Boolean, query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ModernEmptyState(
            icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Inbox,
            title = if (isSearchActive) "未找到相关文章" else "暂无文章",
            description = if (isSearchActive) "没有找到“$query”，换一个关键词继续搜索。" else "添加订阅源，或稍后刷新内容。",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopAppBar(
    state: ReaderContract.State,
    isSearchActive: Boolean,
    showMenuIcon: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onMenuClick: () -> Unit,
    onExport: (ReaderSubscriptionFormat) -> Unit,
    onImport: (ReaderSubscriptionFormat) -> Unit,
) {
    // 搜索状态下的 TopBar
    if (isSearchActive) {
        TopAppBar(
            title = {
                TextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("搜索文章标题或内容...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清空")
                            }
                        }
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    onSearchActiveChange(false)
                    onSearchQueryChanged("") // 退出时清空搜索？或者保留？通常保留状态较好，但这里根据交互需求重置
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出搜索")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            )
        )
    } else {
        var transferMenuExpanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text("阅读器") },
            navigationIcon = {
                if (showMenuIcon) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            },
            actions = {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
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
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
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
        val globalDrawer = LocalAppDrawer.current
        globalDrawer()
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
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = {
            Text("${if (dialog.isImport) "导入" else "导出"} ${dialog.format.name} 订阅")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (dialog.isImport) "粘贴订阅数据；导入会按 ID 或 URL 合并。"
                    else "复制以下内容并保存；该格式可再次导入 Thread。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { if (dialog.isImport) payload = it },
                    readOnly = !dialog.isImport,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text("数据") },
                )
            }
        },
        confirmButton = {
            if (dialog.isImport) {
                Button(
                    onClick = { onImport(payload) },
                    enabled = payload.isNotBlank() && !isWorking,
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("导入")
                    }
                }
            } else {
                Button(onClick = onDismiss) { Text("完成") }
            }
        },
        dismissButton = {
            if (dialog.isImport) TextButton(onClick = onDismiss, enabled = !isWorking) { Text("取消") }
        },
    )
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
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ArticleFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name) },
                leadingIcon = {
                    val icon = when (filter) {
                        ArticleFilter.ALL -> Icons.Default.List
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
