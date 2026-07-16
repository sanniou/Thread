package ai.saniou.thread.feature.bookmark

import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.ThreadCommandBar
import ai.saniou.coreui.widgets.ThreadContentColumn
import ai.saniou.coreui.widgets.ThreadPage
import ai.saniou.coreui.widgets.ThreadSearchField
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.Tag
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
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
import androidx.paging.LoadState.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
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

        ThreadPage {
            ThreadContentColumn(modifier = Modifier.fillMaxSize()) {
                ContextHero(
                    icon = Icons.Default.BookmarkBorder,
                    title = if (state.isSelectionMode) "已选择 ${state.selectedBookmarks.size} 项" else "收藏夹",
                    subtitle = if (state.isSelectionMode) "可以批量删除所选内容" else "集中管理帖子、文章、链接与摘录",
                    metric = if (state.isSelectionMode) {
                        "${state.selectedBookmarks.size} SELECTED"
                    } else {
                        "${lazyPagingItems.itemCount} ITEMS"
                    },
                    actions = {
                        if (state.isSelectionMode) {
                            IconButton(onClick = { viewModel.onEvent(BookmarkContract.Event.DeleteSelectedBookmarks) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除所选")
                            }
                            IconButton(onClick = { viewModel.onEvent(BookmarkContract.Event.ToggleSelectionMode) }) {
                                Icon(Icons.Default.Close, contentDescription = "退出选择")
                            }
                        } else {
                            TextButton(onClick = { viewModel.onEvent(BookmarkContract.Event.ToggleSelectionMode) }) {
                                Text("编辑")
                            }
                        }
                    },
                )
                ThreadCommandBar(
                    primary = {
                        ThreadSearchField(
                            query = state.searchQuery,
                            onQueryChange = {
                                viewModel.onEvent(BookmarkContract.Event.OnSearchQueryChanged(it))
                            },
                            placeholder = "搜索收藏内容",
                        )
                    },
                    secondary = if (state.allTags.isEmpty()) null else {
                        { BookmarkTagFilters(state, viewModel::onEvent) }
                    },
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = Dimens.page_vertical)
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
                                        viewModel.onEvent(
                                            BookmarkContract.Event.ToggleBookmarkSelection(
                                                bookmark.id
                                            )
                                        )
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
                                        viewModel.onEvent(
                                            BookmarkContract.Event.ToggleBookmarkSelection(
                                                bookmark.id
                                            )
                                        )
                                    }
                                },
                                selectedTagIds = state.selectedTags.mapTo(mutableSetOf()) { it.id },
                                onTagClick = { tag ->
                                    val event = if (tag in state.selectedTags) {
                                        BookmarkContract.Event.OnTagDeselected(tag)
                                    } else {
                                        BookmarkContract.Event.OnTagSelected(tag)
                                    }
                                    viewModel.onEvent(event)
                                },
                            )
                        }
                    }
                    item { PagingAppendState(lazyPagingItems) }

                    lazyPagingItems.loadState.apply {
                        when {
                            refresh is Loading -> {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(
                                                Alignment.Center
                                            )
                                        )
                                    }
                                }
                            }

                            refresh is Error -> {
                                val e = lazyPagingItems.loadState.refresh as Error
                                item {
                                    Text(
                                        text = "加载失败: ${e.error.message}",
                                        modifier = Modifier.fillParentMaxSize(),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            refresh is NotLoading && lazyPagingItems.itemCount == 0 -> {
                                item {
                                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                        ModernEmptyState(
                                            icon = Icons.Default.BookmarkBorder,
                                            title = "还没有收藏",
                                            description = "在帖子或文章中收藏内容，它们会出现在这里。",
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
    }

@Composable
private fun BookmarkTagFilters(
    state: BookmarkContract.State,
    onEvent: (BookmarkContract.Event) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.allTags) { tag ->
            val isSelected = tag in state.selectedTags
            FilterChip(
                selected = isSelected,
                onClick = {
                    onEvent(
                        if (isSelected) {
                            BookmarkContract.Event.OnTagDeselected(tag)
                        } else {
                            BookmarkContract.Event.OnTagSelected(tag)
                        },
                    )
                },
                label = { Text(tag.name) },
            )
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onBookmarkClick: (Bookmark) -> Unit,
    onLongClick: () -> Unit,
    selectedTagIds: Set<String>,
    onTagClick: (Tag) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onBookmarkClick(bookmark) },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        }
    ) {
        Row(modifier = Modifier.padding(20.dp)) {
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
                        Text(
                            text = bookmark.content,
                            maxLines = 5,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$label: ${bookmark.sourceType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is Bookmark.Link -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            bookmark.favicon?.takeIf(String::isNotBlank)?.let { favicon ->
                                NetworkImage(
                                    favicon,
                                    contentDescription = "Favicon",
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
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
                        Text(
                            text = "媒体: ${bookmark.url}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (bookmark.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bookmark.tags.forEach { tag ->
                            FilterChip(
                                selected = tag.id in selectedTagIds,
                                onClick = { onTagClick(tag) },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}
