package ai.saniou.coreui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    onRefreshTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    Box(
        modifier = modifier
            .pullToRefresh(isRefreshing, state = refreshState) {
                onRefreshTrigger()
            }
    ) {
        content()
        Indicator(
            refreshState, isRefreshing,
        )
    }
}
