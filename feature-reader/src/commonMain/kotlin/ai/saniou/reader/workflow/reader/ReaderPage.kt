package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import androidx.compose.foundation.clickable
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import coil3.compose.AsyncImage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ReaderPage : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel<ReaderViewModel>()
        val state by viewModel.state.collectAsState()
        val articles = viewModel.articles.collectAsLazyPagingItems()
        var isAddSheetShown by remember { mutableStateOf(false) }
        var editingSource by remember { mutableStateOf<FeedSource?>(null) }

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
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onEvent(ReaderContract.Event.OnSearchQueryChanged(it)) },
                            placeholder = { Text("Search articles...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                            singleLine = true
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh All")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editingSource = null // 确保是添加模式
                    isAddSheetShown = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Feed")
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Sidebar
                LazyColumn(modifier = Modifier.width(250.dp).fillMaxHeight()) {
                    item {
                        FeedSourceItem(
                            source = FeedSource(id = "all", name = "All", url = "", type = ai.saniou.thread.domain.model.FeedType.RSS),
                            unreadCount = state.articleCounts.values.sumOf { it.second },
                            isSelected = state.selectedFeedSourceId == null,
                            onClick = { viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(null)) },
                            onEdit = {},
                            onDelete = {},
                            onRefresh = {}
                        )
                    }
                    items(state.feedSources) { source ->
                        val counts = state.articleCounts[source.id]
                        FeedSourceItem(
                            source = source,
                            unreadCount = counts?.second ?: 0,
                            isSelected = state.selectedFeedSourceId == source.id,
                            onClick = { viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(source.id)) },
                            onEdit = { editingSource = source },
                            onDelete = { viewModel.onEvent(ReaderContract.Event.OnDeleteSource(source.id)) },
                            onRefresh = { viewModel.onEvent(ReaderContract.Event.OnRefreshFeedSource(source.id)) }
                        )
                    }
                }

                // Article List
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    FilterChips(
                        selectedFilter = state.articleFilter,
                        onFilterChange = { viewModel.onEvent(ReaderContract.Event.OnFilterChanged(it)) }
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (val refreshState = articles.loadState.refresh) {
                            is LoadStateLoading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            is LoadStateError -> {
                                Text(
                                    text = "Error: ${refreshState.error.message}",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            else -> {
                                if (articles.itemCount == 0) {
                                    Text(
                                        text = "No articles found.",
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(articles.itemCount) { index ->
                                            val article = articles[index]
                                            if (article != null) {
                                                val sourceName = state.feedSources.find { it.id == article.feedSourceId }?.name ?: "Unknown Source"
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
            modifier = Modifier.weight(1f)
        )
        if (unreadCount > 0) {
            Badge { Text(unreadCount.toString()) }
        }
        if (source.isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }

    if (source.id != "all") {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onEdit()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Refresh") },
                onClick = {
                    onRefresh()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun ArticleItem(article: Article, sourceName: String, onClick: () -> Unit) {
    val titleColor = if (article.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold
    val publishDate = remember(article.publishDate) {
        article.publishDate
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace("T", " ")
            .substringBeforeLast(":") // Format to HH:mm
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = sourceName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )

            if (!article.author.isNullOrBlank()) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = article.author!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = publishDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            if (article.isBookmarked) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Bookmarked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = fontWeight,
            color = titleColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        RichText(
            text = article.content,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            color = titleColor
        )
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
        ArticleFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name) },
                leadingIcon = {
                    when (filter) {
                        ArticleFilter.ALL -> Icon(Icons.Default.List, contentDescription = "All")
                        ArticleFilter.UNREAD -> Icon(Icons.Default.Inbox, contentDescription = "Unread")
                        ArticleFilter.BOOKMARKED -> Icon(Icons.Default.Bookmark, contentDescription = "Bookmarked")
                    }
                }
            )
        }
    }
}
