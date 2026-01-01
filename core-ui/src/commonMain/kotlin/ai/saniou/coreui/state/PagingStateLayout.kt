package ai.saniou.coreui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.LazyPagingItems

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
            refreshState is LoadStateLoading -> {
                if (loading != null) {
                    loading()
                } else {
                    DefaultLoading()
                }
            }

            refreshState is LoadStateError -> {
                val appError = refreshState.error.toAppError(onRetry)
                if (error != null) {
                    error(appError)
                } else {
                    DefaultError(appError, onRetry)
                }
            }

            // Empty State Handling
            // Only show empty state if not loading and itemCount is 0
            items.itemCount == 0 && refreshState !is LoadStateLoading -> {
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
