package ai.saniou.coreui.state

import ai.saniou.coreui.widgets.SaniouErrorPage
import ai.saniou.coreui.widgets.SaniouPageLoadingIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

sealed class LoadableState<T> private constructor(open val data: T?) {

    class Loading<T> : LoadableState<T>(null)

    class Error<T> : LoadableState<T>(null)

    class Success<T>(override val data: T) : LoadableState<T>(data)
}

fun <T> MutableStateFlow<LoadableState<T>>.updateUiState(updater: (T) -> T) {
    this.update { loadableState ->
        if (loadableState is LoadableState.Success) {
            LoadableState.Success(updater(loadableState.data))
        } else {
            loadableState
        }
    }
}

@Composable
inline fun <reified ContentType> LoadableLayout(
    modifier: Modifier = Modifier,
    loadableState: LoadableState<ContentType>,
    crossinline onRetryClick: () -> Unit,
    noinline loading: (@Composable BoxScope.() -> Unit)? = null,
    noinline error: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.(ContentType) -> Unit,
) {
    Box(modifier) {
        when (loadableState) {
            is LoadableState.Loading -> {
                loading?.invoke(this) ?: DefaultLoading()
            }

            is LoadableState.Error -> {
                error?.invoke(this) ?: DefaultError(onRetryClick)
            }

            is LoadableState.Success -> {
                content(loadableState.data)
            }
        }
    }
}

@Composable
fun DefaultLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Unspecified)
    ) {
        SaniouPageLoadingIndicator(
            Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun DefaultError(
    error: AppError? = null,
    onRetry: () -> Unit,
    action: @Composable () -> Unit = {},
) {
    SaniouErrorPage(
        error = error ?: AppError(message = "未知错误"),
        onRetryClick = onRetry,
        action = action
    )
}

/**
 * 兼容旧代码的辅助函数
 */
@Composable
inline fun DefaultError(crossinline onRetryClick: () -> Unit) {
    DefaultError(null, { onRetryClick() }, {})
}

inline fun <reified T> MutableStateFlow<LoadableState<T>>.valueOrNull(): T? {
    return if (value is LoadableState.Success) value.data else null
}
