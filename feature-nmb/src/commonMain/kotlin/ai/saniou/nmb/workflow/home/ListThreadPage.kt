package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.ui.components.ForumThreadCard
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.ThreadListSkeleton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.PagingData
import app.cash.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow

@Composable
fun ListThreadPage(
    threadFlow: Flow<PagingData<ThreadWithInformation>>,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (Long, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val threads = threadFlow.collectAsLazyPagingItems()

    PullToRefreshWrapper(
        onRefreshTrigger = { threads.refresh() },
        modifier = modifier
    ) {

        if (threads.loadState.refresh is LoadStateLoading) {
            when {
                threads.loadState.refresh is LoadStateLoading -> ThreadListSkeleton()
                threads.loadState.refresh is LoadStateError -> {
                    Button(
                        onClick = { threads.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("加载失败，点击重试")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Dimens.padding_medium),
                verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                items(threads.itemCount) { index ->
                    val feed = threads[index] ?: return@items
                    ForumThreadCard(
                        thread = feed,
                        onClick = { onThreadClicked(feed.id) },
                        onImageClick = { img, ext -> onImageClick(feed.id, img, ext) }
                    )
                }

                item {
                    when {
                        threads.loadState.append is LoadStateError -> LoadingFailedIndicator()
                        threads.loadState.append is LoadStateLoading -> LoadingIndicator()
                        threads.loadState.append.endOfPaginationReached && threads.itemCount == 0 -> EmptyContent()
                        threads.loadState.append.endOfPaginationReached -> LoadEndIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_large)
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "这里什么都没有\n试着换个板块看看吧",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
