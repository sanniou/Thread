package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                LazyColumn(modifier = Modifier.width(250.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text("All") },
                            modifier = Modifier.combinedClickable(
                                onClick = { viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(null)) }
                            )
                        )
                    }
                    items(state.feedSources) { source ->
                        FeedSourceItem(
                            source = source,
                            onClick = { viewModel.onEvent(ReaderContract.Event.OnSelectFeedSource(source.id)) },
                            onEdit = { editingSource = source },
                            onDelete = { viewModel.onEvent(ReaderContract.Event.OnDeleteSource(source.id)) },
                            onRefresh = { viewModel.onEvent(ReaderContract.Event.OnRefreshFeedSource(source.id)) }
                        )
                    }
                }

                // Article List
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedSourceItem(
    source: FeedSource,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val lastUpdate = remember(source.lastUpdate) {
        if (source.lastUpdate == 0L) "Never"
        else Instant.fromEpochMilliseconds(source.lastUpdate)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .toString()
            .replace("T", " ")
    }

    ListItem(
        headlineContent = { Text(source.name) },
        supportingContent = { Text("Updated: $lastUpdate") },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true }
        ),
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (source.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        }
    )

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

                Text(
                    text = publishDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.weight(1f))

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
}
