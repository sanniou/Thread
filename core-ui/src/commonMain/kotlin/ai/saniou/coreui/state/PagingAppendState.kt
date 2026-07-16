package ai.saniou.coreui.state

import ai.saniou.coreui.widgets.ThreadStatusBanner
import ai.saniou.coreui.widgets.ThreadStatusTone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

/** Shared non-blocking state for the tail of every paged list. */
@Composable
fun <T : Any> PagingAppendState(
    items: LazyPagingItems<T>,
    modifier: Modifier = Modifier,
    showEnd: Boolean = true,
    endLabel: String = "已加载全部内容",
    onRetry: () -> Unit = { items.retry() },
) {
    when (val append = items.loadState.append) {
        is LoadState.Loading -> {
            Row(
                modifier = modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    "正在加载更多",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is LoadState.Error -> {
            val error = append.error.toAppError(onRetry)
            ThreadStatusBanner(
                title = "未能加载更多内容",
                message = error.message,
                tone = ThreadStatusTone.Warning,
                modifier = modifier.padding(vertical = 8.dp),
                actions = { TextButton(onClick = onRetry) { Text("重试") } },
            )
        }
        is LoadState.NotLoading -> {
            if (showEnd && append.endOfPaginationReached && items.itemCount > 0) {
                Row(
                    modifier = modifier.fillMaxWidth().padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        endLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }
            }
        }
    }
}
