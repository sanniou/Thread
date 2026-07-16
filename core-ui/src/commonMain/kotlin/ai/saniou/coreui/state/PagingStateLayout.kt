package ai.saniou.coreui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.compose.LazyPagingItems

/**
 * Paging 3 状态布局组件
 * 统一处理 Paging 3 的 Refresh Loading、Refresh Error 和 Empty 状态
 *
 * @param items 分页数据项
 * @param modifier 修饰符
 * @param onRetry 重试回调，默认调用 items.retry()
 * @param loading 自定义 Loading 视图
 * @param error 自定义 Error 视图
 * @param empty 自定义空数据视图
 * @param content 内容视图
 */
@Composable
fun <T : Any> PagingStateLayout(
    items: LazyPagingItems<T>,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = { items.retry() },
    loading: @Composable (BoxScope.() -> Unit)? = null,
    error: @Composable (BoxScope.(AppError) -> Unit)? = null,
    empty: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable (LazyPagingItems<T>) -> Unit
) {
    Box(modifier = modifier) {
        val refreshState = items.loadState.refresh

        when {
            // Cached content is the primary offline surface. A refresh must never replace usable
            // rows with a full-screen spinner or error page.
            items.itemCount > 0 -> {
                content(items)
                if (refreshState is Error) {
                    val appError = refreshState.error.toAppError(onRetry)
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "正在显示缓存：${appError.message}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            TextButton(onClick = onRetry) { Text("重试") }
                        }
                    }
                }
            }

            refreshState is Loading -> {
                if (loading != null) {
                    loading()
                } else {
                    DefaultLoading()
                }
            }

            refreshState is Error -> {
                val appError = refreshState.error.toAppError(onRetry)
                if (error != null) {
                    error(appError)
                } else {
                    DefaultError(appError, onRetry)
                }
            }

            // Empty State Handling
            // Only show empty state if not loading and itemCount is 0
            items.itemCount == 0 && refreshState !is Loading -> {
                if (empty != null) {
                    empty()
                } else {
                    // Default Empty State can be added here if needed, or fallback to content
                    // For now, keeping it flexible as per previous logic, but ensuring we don't flash empty during initial load
                    content(items)
                }
            }

            else -> {
                content(items)
            }
        }
    }
}
