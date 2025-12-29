package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic as Post
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.PagingData
import app.cash.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow

@Composable
fun ListThreadPage(
    threadFlow: Flow<PagingData<Post>>,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (Long, Image) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null,
    showChannelBadge: Boolean = true,
) {
    val threads = threadFlow.collectAsLazyPagingItems()

    PullToRefreshWrapper(
        onRefreshTrigger = { threads.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        PagingStateLayout(
            items = threads,
            loading = { ThreadListSkeleton() },
            empty = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    StateContent(
                        imageVector = Icons.Outlined.Star,
                        message = "这里什么都没有\n试着换个板块看看吧"
                    )
                }
            }
        ) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(
                    start = Dimens.padding_medium,
                    end = Dimens.padding_medium,
                    top = Dimens.padding_medium,
                    bottom = Dimens.padding_large
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
            ) {
                if (headerContent != null) {
                    item {
                        headerContent()
                    }
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
                        is LoadStateError -> LoadingFailedIndicator()
                        is LoadStateLoading -> LoadingIndicator()
                        else -> LoadEndIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun StateContent(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    message: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_large)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        action?.invoke()
    }
}
