package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.data.entity.ThreadWithInformation
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.ThreadCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow

@Composable
fun ListThreadPage(
    threads: Flow<PagingData<ThreadWithInformation>>,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (Long, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feeds = threads.collectAsLazyPagingItems()
    PullToRefreshWrapper(
        onRefreshTrigger = { feeds.refresh() },
        modifier = modifier
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp)
        ) {
            items(feeds.itemCount) { index ->
                val feed = feeds[index] ?: return@items
                ThreadCard(
                    thread = feed.thread,
                    onClick = { onThreadClicked(feed.thread.id) },
                    onImageClick = { img, ext -> onImageClick(feed.thread.id, img, ext) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                when {
                    feeds.loadState.append is LoadState.Error -> LoadingFailedIndicator()
                    feeds.loadState.append is LoadState.Loading -> LoadingIndicator()
                    feeds.loadState.append.endOfPaginationReached && feeds.itemCount == 0 -> EmptyContent()
                    feeds.loadState.append.endOfPaginationReached -> LoadEndIndicator()
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无内容",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}