package ai.saniou.reader.workflow.reader

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.widgets.RichText
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage

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

        val isSheetShown = isAddSheetShown || editingSource != null

        if (isSheetShown) {
            val addFeedSourceViewModel = remember(editingSource) { AddFeedSourceViewModel(sourceToEdit = editingSource) }
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

        Scaffold(
            topBar = {
                ReaderTopAppBar(
                    state = state,
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it },
                    onSearchQueryChanged = { viewModel.onEvent(ReaderContract.Event.OnSearchQueryChanged(it)) },
                    onRefreshAll = { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editingSource = null // 确保是添加模式
                    isAddSheetShown = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加订阅源")
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Sidebar
                FeedSourceList(
                    sources = state.feedSources,
                    articleCounts = state.articleCounts,
                    selectedSourceId = state.selectedFeedSourceId,
                    onSelect = { viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(it)) },
                    onEdit = { editingSource = it },
                    onDelete = { viewModel.onEvent(ReaderContract.Event.OnDeleteSource(it)) },
                    onRefresh = { viewModel.onEvent(ReaderContract.Event.OnRefreshFeedSource(it)) }
                )

                // Article List
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    FilterChips(
                        selectedFilter = state.articleFilter,
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) }
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        val refreshState = articles.loadState.refresh
                        when {
                            refreshState is LoadStateLoading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            refreshState is LoadStateError -> {
                                Text(
                                    text = "错误: ${refreshState.error.message}",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            articles.itemCount == 0 -> {
                                Text(
                                    text = "没有文章。尝试添加一个订阅源或更改筛选条件。",
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                                )
                            }
                            else -> {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(articles.itemCount) { index ->
                                        val article = articles[index]
                                        if (article != null) {
                                            val sourceName = state.feedSources.find { it.id == article.feedSourceId }?.name ?: "未知来源"
                                            ArticleItem(
                                                article = article,
                                                sourceName = sourceName,
                                                onClick = {
                                                    viewModel.onEvent(ReaderContract.Event.OnMarkArticleAsRead(article.id, true))
                                                    navigator.push(ArticleDetailPage(article.id))
                                                }
                                            )
                                        }
                                    }

                                    if (articles.loadState.append is LoadStateLoading) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopAppBar(
    state: ReaderContract.State,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefreshAll: () -> Unit
) {
    TopAppBar(
        title = {
            AnimatedVisibility(visible = !isSearchActive) {
                Text("阅读器")
            }
        },
        actions = {
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("搜索文章...") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { onSearchActiveChange(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                        }
                    }
                )
            }
            if (!isSearchActive) {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
                IconButton(onClick = onRefreshAll) {
                    Icon(Icons.Default.Refresh, contentDescription = "全部刷新")
                }
            }
        }
    )
}

@Composable
private fun FeedSourceList(
    sources: List<FeedSource>,
    articleCounts: Map<String, Pair<Int, Int>>,
    selectedSourceId: String?,
    onSelect: (String?) -> Unit,
    onEdit: (FeedSource) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.width(250.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        item {
            FeedSourceItem(
                source = FeedSource(id = "all", name = "全部", url = "", type = ai.saniou.thread.domain.model.FeedType.RSS),
                unreadCount = articleCounts.values.sumOf { it.second },
                isSelected = selectedSourceId == null,
                onClick = { onSelect(null) },
                onEdit = {},
                onDelete = {},
                onRefresh = {}
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
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedSourceItem(
    source: FeedSource,
    unreadCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (source.id != "all") showMenu = true }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = source.iconUrl,
            contentDescription = "${source.name} icon",
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = source.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (unreadCount > 0) {
            Badge { Text(unreadCount.toString()) }
        }
        if (source.isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
        if (source.id != "all") {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { onEdit(); showMenu = false })
                    DropdownMenuItem(text = { Text("刷新") }, onClick = { onRefresh(); showMenu = false })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { onDelete(); showMenu = false })
                }
            }
        }
    }
}

@Composable
fun ArticleItem(article: Article, sourceName: String, onClick: () -> Unit) {
    val titleColor = if (article.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = sourceName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            if (!article.author.isNullOrBlank()) {
                Text("·", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = article.author!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = article.publishDate.toRelativeTimeString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = fontWeight,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (article.content.isNotBlank()) {
            RichText(
                text = article.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                color = titleColor.copy(alpha = 0.8f),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedFilter: ArticleFilter,
    onFilterChange: (ArticleFilter) -> Unit
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
                    Icon(icon, contentDescription = filter.name)
                }
            )
        }
    }
}
