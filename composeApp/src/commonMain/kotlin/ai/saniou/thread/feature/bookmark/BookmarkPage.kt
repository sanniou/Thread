package ai.saniou.thread.feature.bookmark

import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.bookmark.Bookmark
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.LoadStateNotLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object BookmarkPage : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: BookmarkViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsState()
        val lazyPagingItems = viewModel.bookmarksFlow.collectAsLazyPagingItems()

        Scaffold(
            topBar = {
                if (state.isSelectionMode) {
                    TopAppBar(
                        title = { Text("已选择 ${state.selectedBookmarks.size} 项") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.onEvent(BookmarkContract.Event.ToggleSelectionMode) }) {
                                Icon(Icons.Default.Delete, contentDescription = "取消选择") // Close icon actually
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.onEvent(BookmarkContract.Event.DeleteSelectedBookmarks) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    )
                } else {
                    SaniouTopAppBar(
                        title = "收藏夹",
                        onNavigationClick = { navigator.pop() },
                        actions = {
                            TextButton(onClick = { viewModel.onEvent(BookmarkContract.Event.ToggleSelectionMode) }) {
                                Text("编辑")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(BookmarkContract.Event.OnSearchQueryChanged(it)) },
                    label = { Text("搜索收藏") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                // Tag Filters
                if (state.allTags.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.allTags) { tag ->
                            val isSelected = state.selectedTags.contains(tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        viewModel.onEvent(BookmarkContract.Event.OnTagDeselected(tag))
                                    } else {
                                        viewModel.onEvent(BookmarkContract.Event.OnTagSelected(tag))
                                    }
                                },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.id }
                    ) { index ->
                        val bookmark = lazyPagingItems[index]
                        if (bookmark != null) {
                            val isSelected = state.selectedBookmarks.contains(bookmark.id)
                            BookmarkItem(
                                bookmark = bookmark,
                                isSelectionMode = state.isSelectionMode,
                                isSelected = isSelected,
                                onBookmarkClick = {
                                    if (state.isSelectionMode) {
                                        viewModel.onEvent(BookmarkContract.Event.ToggleBookmarkSelection(bookmark.id))
                                    } else {
                                        when (it) {
                                            is Bookmark.Quote -> {
                                                if (it.sourceType == "article") {
                                                    navigator.push(ArticleDetailPage(it.sourceId))
                                                } else {
                                                    navigator.push(TopicDetailPage(it.sourceId.toLong()))
                                                }
                                            }

                                            is Bookmark.Link -> {
                                                // Open URL logic (e.g., WebView or Browser)
                                            }
                                            // ... other types
                                            else -> {}
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!state.isSelectionMode) {
                                        viewModel.onEvent(BookmarkContract.Event.ToggleSelectionMode)
                                        viewModel.onEvent(BookmarkContract.Event.ToggleBookmarkSelection(bookmark.id))
                                    }
                                }
                            )
                        }
                    }

                    lazyPagingItems.loadState.apply {
                        when {
                            refresh is LoadStateLoading || append is LoadStateLoading -> {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }

                            refresh is LoadStateError -> {
                                val e = lazyPagingItems.loadState.refresh as LoadStateError
                                item {
                                    Text(
                                        text = "加载失败: ${e.error.message}",
                                        modifier = Modifier.fillParentMaxSize(),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            append is LoadStateError -> {
                                val e = lazyPagingItems.loadState.append as LoadStateError
                                item {
                                    Text(
                                        text = "加载更多失败: ${e.error.message}",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { lazyPagingItems.retry() }
                                    )
                                }
                            }

                            refresh is LoadStateNotLoading && lazyPagingItems.itemCount == 0 -> {
                                item {
                                    Text(
                                        text = "没有收藏",
                                        modifier = Modifier.fillParentMaxSize(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onBookmarkClick: (Bookmark) -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onBookmarkClick(bookmark) },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else CardDefaults.cardColors()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                when (bookmark) {
                    is Bookmark.Text -> {
                        Text(text = bookmark.content, style = MaterialTheme.typography.bodyLarge)
                    }

                    is Bookmark.Quote -> {
                        val label = if (bookmark.sourceType == "article") "文章" else "帖子"
                        Text(text = bookmark.content, maxLines = 5, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$label: ${bookmark.sourceType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is Bookmark.Link -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NetworkImage(
                                bookmark.favicon!!,
                                contentDescription = "Favicon",
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bookmark.title ?: bookmark.url,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                        if (bookmark.description != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = bookmark.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is Bookmark.Image -> {
                        NetworkImage(
                            imageUrl = bookmark.url,
                            contentDescription = "图片收藏",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    is Bookmark.Media -> {
                        Text(text = "媒体: ${bookmark.url}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (bookmark.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bookmark.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { /* TODO: Handle tag click */ },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}
