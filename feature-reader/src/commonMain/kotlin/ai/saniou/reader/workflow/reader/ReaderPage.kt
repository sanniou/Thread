package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.composition.LocalAppDrawer
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AppDrawerItem
import ai.saniou.coreui.widgets.ArticleItem
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import androidx.compose.foundation.layout.*
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
                    }
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
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) }
                    )
                }
            } else {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(modifier = Modifier.width(280.dp)) {
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
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) }
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
) {
    Scaffold(
        topBar = {
            ReaderTopAppBar(
                state = state,
                isSearchActive = isSearchActive,
                showMenuIcon = isMobile,
                onSearchActiveChange = onSearchActiveChange,
                onSearchQueryChanged = onSearchQueryChanged,
                onRefreshAll = onRefreshAll,
                onMenuClick = onMenuClick
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            FilterChips(
                selectedFilter = state.articleFilter,
                onFilterChange = onFilterChange
            )
            PagingStateLayout(
                items = articles,
                modifier = Modifier.weight(1f).fillMaxSize(),
                loading = { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
                empty = {
                    EmptyState(
                        isSearchActive = state.searchQuery.isNotEmpty(),
                        query = state.searchQuery
                    )
                }
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Inbox
        val title = if (isSearchActive) "未找到相关文章" else "暂无文章"
        val subtitle =
            if (isSearchActive) "尝试使用不同的关键词" else "尝试添加新的订阅源或稍后刷新"

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    } else {
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
            }
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
) {
    Column(modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp)) {
        val globalDrawer = LocalAppDrawer.current
        globalDrawer()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "订阅源",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.primary
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

        AppDrawerItem(
            label = "添加订阅源",
            icon = Icons.Default.Add,
            selected = false,
            onClick = onAdd
        )
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
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
