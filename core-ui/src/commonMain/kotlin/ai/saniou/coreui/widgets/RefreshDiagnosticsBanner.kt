package ai.saniou.coreui.widgets

import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshTaskState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun RefreshDiagnosticsBanner(
    failures: List<RefreshTaskState>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    if (failures.isEmpty()) return
    var expanded by remember(failures.map(RefreshTaskState::key)) { mutableStateOf(failures.size == 1) }
    val offlineCount = failures.count { it.failureKind == RefreshFailureKind.OFFLINE }
    val authCount = failures.count { it.failureKind == RefreshFailureKind.AUTHENTICATION }
    val summary = when {
        offlineCount == failures.size -> "设备离线；已有缓存仍然可以继续阅读。"
        authCount > 0 -> "$authCount 个来源需要更新登录状态，其他来源不受影响。"
        else -> "失败已按来源隔离，成功来源与本地内容继续显示。"
    }
    val tone = if (authCount > 0) ThreadStatusTone.Error else ThreadStatusTone.Warning

    ThreadStatusBanner(
        title = "${failures.size} 个来源暂未刷新",
        message = summary,
        modifier = modifier,
        tone = tone,
        icon = if (offlineCount == failures.size) Icons.Default.CloudOff else Icons.Default.SyncProblem,
        actions = {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起" else "详情")
            }
            onRetry?.let { retry ->
                TextButton(onClick = retry) { Text("重试") }
            }
        },
        details = {
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    failures.forEach { failure -> FailureRow(failure) }
                }
            }
        },
    )
}

@Composable
private fun FailureRow(failure: RefreshTaskState) {
    val presentation = failure.failureKind.presentation()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                presentation.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Column(Modifier.weight(1f)) {
                Text(failure.label, style = MaterialTheme.typography.labelMedium)
                Text(
                    buildString {
                        append(presentation.label)
                        failure.message?.takeIf(String::isNotBlank)?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${failure.attempt} TRY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class FailurePresentation(val label: String, val icon: ImageVector)

private fun RefreshFailureKind?.presentation(): FailurePresentation = when (this) {
    RefreshFailureKind.OFFLINE -> FailurePresentation("网络离线", Icons.Default.CloudOff)
    RefreshFailureKind.TIMEOUT -> FailurePresentation("请求超时", Icons.Default.HourglassTop)
    RefreshFailureKind.RATE_LIMIT -> FailurePresentation("请求受限", Icons.Default.HourglassTop)
    RefreshFailureKind.AUTHENTICATION -> FailurePresentation("登录失效", Icons.Default.Lock)
    RefreshFailureKind.REMOTE -> FailurePresentation("服务异常", Icons.Default.SyncProblem)
    RefreshFailureKind.UNKNOWN, null -> FailurePresentation("未知错误", Icons.Default.WarningAmber)
}
