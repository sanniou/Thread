package ai.saniou.coreui.state

import androidx.compose.foundation.layout.Box
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
    loading: @Composable (() -> Unit)? = null,
    error: @Composable ((AppError) -> Unit)? = null,
    empty: @Composable (() -> Unit)? = null,
    content: @Composable (LazyPagingItems<T>) -> Unit
) {
    Box(modifier = modifier) {
        val refreshState = items.loadState.refresh
        
        when {
            refreshState is LoadStateLoading -> {
                loading?.invoke() ?: DefaultLoading()
            }

            refreshState is LoadStateError -> {
                val appError = refreshState.error.toAppError(onRetry)
                if (error != null) {
                    error(appError)
                } else {
                    DefaultError(appError, onRetry)
                }
            }

            items.itemCount == 0 -> {
                if (empty != null) {
                    empty()
                } else {
                    // 默认不显示空视图，或者可以提供一个默认的 EmptyView
                    // 这里暂时什么都不显示，或者显示内容（内容可能会处理空列表）
                    // 考虑到 Paging 通常会有空列表的情况，如果 content 内部能处理空列表最好
                    // 但 PagingStateLayout 的目的是处理加载和错误，空状态也是一种状态
                    // 如果提供了 empty，则显示 empty，否则显示 content（也许 content 是一个 EmptyView?）
                    // 更好的做法：如果 itemCount == 0 且 refresh 是 NotLoading，则确实是空数据
                    // 很多时候空数据也需要显示一个提示
                    // 让我们留给调用者决定，如果 empty 不为空则显示，否则 content
                    content(items)
                }
            }

            else -> {
                content(items)
            }
        }
    }
}