package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.forum.ui.components.ForumThreadCard
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Post
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
import app.cash.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun ListThreadPageOld(
    threadFlow: Flow<PagingData<ThreadWithInformation>>,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (Long, String, String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    ListThreadPage(
        threadFlow = threadFlow.map { it.map { it.toDomain() } },
        onThreadClicked = onThreadClicked,
        onImageClick = onImageClick,
        onUserClick = onUserClick,
        modifier = modifier,
        state = state,
    )
}

@Composable
fun ListThreadPage(
    threadFlow: Flow<PagingData<Post>>,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (Long, String, String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
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
                contentPadding = PaddingValues(horizontal = Dimens.padding_medium),
                verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                items(threads.itemCount) { index ->
                    val feed = threads[index] ?: return@items
                    ForumThreadCard(
                        thread = feed,
                        onClick = { onThreadClicked(feed.id.toLong()) },
                        onImageClick = { img, ext -> onImageClick(feed.id.toLong(), img, ext) },
                        onUserClick = onUserClick
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
