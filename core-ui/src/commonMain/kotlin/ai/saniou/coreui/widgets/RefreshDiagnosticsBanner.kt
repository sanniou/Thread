package ai.saniou.coreui.widgets

import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshTaskState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RefreshDiagnosticsBanner(
    failures: List<RefreshTaskState>,
    modifier: Modifier = Modifier,
) {
    if (failures.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("部分内容刷新失败", style = MaterialTheme.typography.labelLarge)
            failures.take(3).forEach { failure ->
                Text(
                    text = "${failure.label} · ${failure.failureKind.displayName()} · ${failure.message.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun RefreshFailureKind?.displayName(): String = when (this) {
    RefreshFailureKind.OFFLINE -> "网络离线"
    RefreshFailureKind.TIMEOUT -> "请求超时"
    RefreshFailureKind.RATE_LIMIT -> "请求受限"
    RefreshFailureKind.AUTHENTICATION -> "登录失效"
    RefreshFailureKind.REMOTE -> "服务异常"
    RefreshFailureKind.UNKNOWN, null -> "未知错误"
}
