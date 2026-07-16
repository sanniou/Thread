package ai.saniou.coreui.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview

/** Retains an independent list position for every source/filter scope while the page is alive. */
@OptIn(FlowPreview::class)
@Composable
fun KeyedLazyListState(
    stateKey: String,
    initialIndex: Int = 0,
    initialOffset: Int = 0,
    onPositionChanged: ((index: Int, offset: Int) -> Unit)? = null,
    content: @Composable (LazyListState) -> Unit,
) {
    val stateHolder = rememberSaveableStateHolder()
    val latestOnPositionChanged by rememberUpdatedState(onPositionChanged)
    stateHolder.SaveableStateProvider(stateKey) {
        val state = rememberLazyListState(
            initialFirstVisibleItemIndex = initialIndex.coerceAtLeast(0),
            initialFirstVisibleItemScrollOffset = initialOffset.coerceAtLeast(0),
        )
        LaunchedEffect(state, stateKey) {
            if (latestOnPositionChanged == null) return@LaunchedEffect
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .debounce(POSITION_PERSIST_DEBOUNCE_MILLIS)
                .collect { (index, offset) -> latestOnPositionChanged?.invoke(index, offset) }
        }
        content(state)
    }
}

private const val POSITION_PERSIST_DEBOUNCE_MILLIS = 450L
