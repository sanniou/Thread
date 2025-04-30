package ai.saniou.coreui.state

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface UiStateWrapper {
    object Loading : UiStateWrapper
    data class Success<T>(val value: T?) : UiStateWrapper
    data class Error(val throwable: Throwable, val message: String?) : UiStateWrapper
}

inline fun <reified T : UiStateWrapper> MutableStateFlow<UiStateWrapper>.updateUiState(
    block: (T) -> T
) {
    this.update { uiStateWrapper ->
        if (uiStateWrapper is T) {
            block(uiStateWrapper)
        } else {
            uiStateWrapper
        }
    }
}

@Composable
inline fun <reified T : UiStateWrapper> UiStateWrapper.LoadingWrapper(
    content: @Composable (T) -> Unit,
    noinline error: @Composable (() -> Unit)? = null,
    crossinline onRetryClick: () -> Unit
) {
    when (this) {
        is UiStateWrapper.Loading -> {
            DefaultLoading()
        }

        is UiStateWrapper.Error -> {
            error?.invoke() ?: DefaultError(onRetryClick)
        }

        is T -> {
            content(this)
        }

        is UiStateWrapper.Success<*> -> {
            content(this.value as T)
        }
    }
}
