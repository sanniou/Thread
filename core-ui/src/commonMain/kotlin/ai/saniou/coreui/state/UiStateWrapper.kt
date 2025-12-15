package ai.saniou.coreui.state

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

sealed interface UiStateWrapper<out T> {
    data object Loading : UiStateWrapper<Nothing>
    data class Success<T>(val value: T) : UiStateWrapper<T>
    data class Error(val error: AppError) : UiStateWrapper<Nothing>
}

inline fun <T> MutableStateFlow<UiStateWrapper<T>>.updateData(
    block: (T) -> T
) {
    this.update { state ->
        if (state is UiStateWrapper.Success) {
            UiStateWrapper.Success(block(state.value))
        } else {
            state
        }
    }
}

/**
 * 状态布局组件
 * 统一处理 Loading、Error 和 Success 状态的显示
 *
 * @param state 当前 UI 状态
 * @param onRetry 点击重试按钮的回调（通常用于 Error 状态）
 * @param loading 自定义 Loading 视图，为空则使用默认 Loading
 * @param error 自定义 Error 视图，为空则使用默认 ErrorPage
 * @param content Success 状态下的内容视图
 */
@Composable
fun <T> StateLayout(
    state: UiStateWrapper<T>,
    onRetry: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    loading: @Composable (() -> Unit)? = null,
    error: @Composable ((AppError) -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        when (state) {
            is UiStateWrapper.Loading -> {
                loading?.invoke() ?: DefaultLoading()
            }

            is UiStateWrapper.Error -> {
                // 如果 Error 中自带了 onRetry，优先使用；否则使用外部传入的 onRetry
                val errorState = state.error
                val finalRetry = errorState.onRetry ?: onRetry
                
                if (error != null) {
                    error(errorState)
                } else {
                    DefaultError(state.error, finalRetry)
                }
            }

            is UiStateWrapper.Success -> {
                content(state.value)
            }
        }
    }
}
