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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
        val articles = state.articles.collectAsLazyPagingItems()

        if (state.isDialogShown) {
            FeedSourceDialog(
                source = state.editingSource,
                onDismiss = { viewModel.onEvent(ReaderContract.Event.OnDismissDialog) },
                onConfirm = { source -> viewModel.onEvent(ReaderContract.Event.OnSaveSource(source)) }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reader") },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(ReaderContract.Event.OnRefreshAll) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh All")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.onEvent(ReaderContract.Event.OnShowAddDialog) }) {
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
                            onEdit = { viewModel.onEvent(ReaderContract.Event.OnShowEditDialog(source)) },
                            onDelete = { viewModel.onEvent(ReaderContract.Event.OnDeleteSource(source.id)) }
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
                                            ArticleItem(
                                                article = article,
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
    onDelete: () -> Unit
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
            text = { Text("Delete") },
            onClick = {
                onDelete()
                showMenu = false
            }
        )
    }
}

@Composable
fun ArticleItem(article: Article, onClick: () -> Unit) {
    val titleColor = if (article.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = fontWeight,
                color = titleColor
            )
            RichText(
                text = article.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                color = titleColor
            )
        }
    }
}
