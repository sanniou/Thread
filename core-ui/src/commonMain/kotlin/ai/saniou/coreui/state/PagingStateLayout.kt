package ai.saniou.coreui.state

import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadStatusBanner
import ai.saniou.coreui.widgets.ThreadStatusTone
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.core_ui_no_internet_retry
import thread.core_ui.generated.resources.s_e3ce4f0d76

enum class PagingContentState {
    CachedContent,
    InitialLoading,
    BlockingError,
    Empty,
    Content,
}

/** Pure policy used by UI and Desktop tests: usable rows always beat refresh chrome. */
fun resolvePagingContentState(itemCount: Int, refreshState: LoadState): PagingContentState = when {
    itemCount > 0 -> PagingContentState.CachedContent
    refreshState is Loading -> PagingContentState.InitialLoading
    refreshState is Error -> PagingContentState.BlockingError
    refreshState is LoadState.NotLoading -> PagingContentState.Empty
    else -> PagingContentState.Content
}

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

        when (resolvePagingContentState(items.itemCount, refreshState)) {
            // Cached content is the primary offline surface. A refresh must never replace usable
            // rows with a full-screen spinner or error page.
            PagingContentState.CachedContent -> {
                content(items)
                if (refreshState is Error) {
                    val appError = refreshState.error.toAppError(onRetry)
                    ThreadStatusBanner(
                        title = stringResource(Res.string.s_e3ce4f0d76),
                        message = appError.message,
                        tone = if (appError.type == AppErrorType.NETWORK) {
                            ThreadStatusTone.Warning
                        } else {
                            ThreadStatusTone.Error
                        },
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).widthIn(max = 720.dp),
                        actions = {
                            SaniouTextButton(onClick = onRetry, text = stringResource(Res.string.core_ui_no_internet_retry))
                        },
                    )
                }
            }

            PagingContentState.InitialLoading -> {
                if (loading != null) {
                    loading()
                } else {
                    DefaultLoading()
                }
            }

            PagingContentState.BlockingError -> {
                check(refreshState is Error)
                val appError = refreshState.error.toAppError(onRetry)
                if (error != null) {
                    error(appError)
                } else {
                    DefaultError(appError, onRetry)
                }
            }

            // Empty State Handling
            // Only show empty state if not loading and itemCount is 0
            PagingContentState.Empty -> {
                if (empty != null) {
                    empty()
                } else {
                    // Default Empty State can be added here if needed, or fallback to content
                    // For now, keeping it flexible as per previous logic, but ensuring we don't flash empty during initial load
                    content(items)
                }
            }

            PagingContentState.Content -> {
                content(items)
            }
        }
    }
}
