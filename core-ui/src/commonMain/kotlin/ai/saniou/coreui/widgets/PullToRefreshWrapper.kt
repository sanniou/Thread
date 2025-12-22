package ai.saniou.coreui.widgets

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean = false,
    onRefreshTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    // 关键改进：开放 Indicator 插槽，允许自定义风格
    indicator: @Composable BoxScope.(PullToRefreshState) -> Unit = { state ->
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // 适配 MD3 颜色
            color = MaterialTheme.colorScheme.primary
        )
    },
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()

    // 使用最新的 PullToRefreshBox (如果 CMP 版本支持)
    // 或者保持 Box 结构但增加 Haptic 反馈
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshTrigger,
        state = state,
        modifier = modifier,
        indicator = { indicator(state) }
    ) {
        content()
    }
}
