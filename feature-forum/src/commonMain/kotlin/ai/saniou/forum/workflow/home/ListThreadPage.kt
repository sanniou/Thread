package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.DefaultError
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow

@Composable
fun ListThreadPage(
    threadFlow: Flow<PagingData<Topic>>,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (Long, Image) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null,
    showChannelBadge: Boolean = true,
    onShowCache: (() -> Unit)? = null,
) {
    val threads = threadFlow.collectAsLazyPagingItems()
    val windowInfo = LocalThreadWindowInfo.current

    PullToRefreshWrapper(
        onRefreshTrigger = { threads.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            PagingStateLayout(
                items = threads,
                loading = { ThreadListSkeleton() },
                error = { appError ->
                    DefaultError(
                        error = appError,
                        onRetry = { threads.retry() },
                        action = {
                            if (onShowCache != null) {
                                TextButton(onClick = onShowCache) {
                                    Text("显示缓存")
                                }
                            }
                        }
                    )
                },
                empty = {
                    ModernEmptyState(
                        icon = Icons.Outlined.Star,
                        title = "这里还没有主题",
                        description = "探索其他版块，或成为第一个发帖的人。",
                        modifier = Modifier.align(Alignment.Center),
                    )
                },
                modifier = Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = Dimens.contentMaxWidth),
            ) {
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(
                        start = windowInfo.pageHorizontalPadding,
                        end = windowInfo.pageHorizontalPadding,
                        top = Dimens.padding_medium,
                        bottom = Dimens.page_vertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
                ) {
                    if (headerContent != null) {
                        item { headerContent() }
                    }

                    items(threads.itemCount) { index ->
                        val feed = threads[index] ?: return@items
                        TopicCard(
                            topic = feed,
                            onClick = { onThreadClicked(feed.id.toLong()) },
                            onImageClick = { img -> onImageClick(feed.id.toLong(), img) },
                            onUserClick = onUserClick,
                            showChannelBadge = showChannelBadge
                        )
                    }

                    item {
                        when (threads.loadState.append) {
                            is Error -> LoadingFailedIndicator(
                                onClick = { threads.retry() },
                                onShowCache = onShowCache
                            )

                            is Loading -> LoadingIndicator()
                            else -> LoadEndIndicator()
                        }
                    }
                }
            }
        }
    }
}
