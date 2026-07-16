package ai.saniou.coreui.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

/** Retains an independent list position for every source/filter scope while the page is alive. */
@Composable
fun KeyedLazyListState(
    stateKey: String,
    content: @Composable (LazyListState) -> Unit,
) {
    val stateHolder = rememberSaveableStateHolder()
    stateHolder.SaveableStateProvider(stateKey) {
        content(rememberLazyListState())
    }
}
