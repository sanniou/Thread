package ai.saniou.nmb.workflow.bookmark

import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.thread.domain.model.Bookmark
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ai.saniou.coreui.widgets.NetworkImage
import coil3.compose.AsyncImage

object BookmarkPage : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: BookmarkViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsState()
        val lazyPagingItems = state.bookmarks.collectAsLazyPagingItems()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("收藏夹") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
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
                            BookmarkItem(bookmark = bookmark, onBookmarkClick = {
                                when (it) {
                                    is Bookmark.Quote -> navigator.push(ThreadPage(it.sourceId.toLong()))
                                    else -> {
                                        // 其他类型的点击事件
                                    }
                                }
                            })
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
                                        text = "加载失败: ${e.error.localizedMessage}",
                                        modifier = Modifier.fillParentMaxSize(),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            append is LoadStateError -> {
                                val e = lazyPagingItems.loadState.append as LoadStateError
                                item {
                                    Text(
                                        text = "加载更多失败: ${e.error.localizedMessage}",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { lazyPagingItems.retry() }
                                    )
                                }
                            }
                            refresh is app.cash.paging.LoadStateNotLoading && lazyPagingItems.itemCount == 0 -> {
                                item {
                                    Text(
                                        text = "没有收藏",
                                        modifier = Modifier.fillParentMaxSize()
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookmarkItem(bookmark: Bookmark, onBookmarkClick: (Bookmark) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookmarkClick(bookmark) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (bookmark) {
                is Bookmark.Text -> {
                    Text(text = bookmark.content, style = MaterialTheme.typography.bodyLarge)
                }
                is Bookmark.Quote -> {
                    Text(text = bookmark.content, maxLines = 5, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "引用自: ${bookmark.sourceType} ${bookmark.sourceId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is Bookmark.Link -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = bookmark.favicon,
                            contentDescription = "Favicon",
                            modifier = Modifier.size(24.dp)
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
                    // 可在此处添加视频或音频播放器预览
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
