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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.core_ui_no_internet_retry
import thread.core_ui.generated.resources.s_316334eed7
import thread.core_ui.generated.resources.s_4f55ee1e68
import thread.core_ui.generated.resources.s_5d5815647c
import thread.core_ui.generated.resources.s_64f6dfa563
import thread.core_ui.generated.resources.s_65c709f5be
import thread.core_ui.generated.resources.s_d5f066a88a
import thread.core_ui.generated.resources.s_ed7f8c8bad
import thread.core_ui.generated.resources.error_unknown
import thread.core_ui.generated.resources.error_network_offline
import thread.core_ui.generated.resources.error_rate_limited
import thread.core_ui.generated.resources.s_ab0a60e784
import thread.core_ui.generated.resources.error_service
import thread.core_ui.generated.resources.s_e41ce24cf7

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
        offlineCount == failures.size -> stringResource(Res.string.s_d5f066a88a)
        authCount > 0 -> stringResource(Res.string.s_316334eed7, authCount)
        else -> stringResource(Res.string.s_ed7f8c8bad)
    }
    val tone = if (authCount > 0) ThreadStatusTone.Error else ThreadStatusTone.Warning

    ThreadStatusBanner(
        title = stringResource(Res.string.s_64f6dfa563, failures.size),
        message = summary,
        modifier = modifier,
        tone = tone,
        icon = if (offlineCount == failures.size) Icons.Default.CloudOff else Icons.Default.SyncProblem,
        actions = {
            SaniouTextButton(
                onClick = { expanded = !expanded },
                text = if (expanded) stringResource(Res.string.s_5d5815647c) else stringResource(Res.string.s_4f55ee1e68),
            )
            onRetry?.let { retry ->
                SaniouTextButton(onClick = retry, text = stringResource(Res.string.core_ui_no_internet_retry))
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
                Text(
                    failure.label,
                    style = MaterialTheme.typography.labelMedium,
                )
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
                stringResource(Res.string.s_65c709f5be, failure.attempt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class FailurePresentation(val label: String, val icon: ImageVector)

@Composable
private fun RefreshFailureKind?.presentation(): FailurePresentation = when (this) {
    RefreshFailureKind.OFFLINE -> FailurePresentation(stringResource(Res.string.error_network_offline), Icons.Default.CloudOff)
    RefreshFailureKind.TIMEOUT -> FailurePresentation(stringResource(Res.string.s_e41ce24cf7), Icons.Default.HourglassTop)
    RefreshFailureKind.RATE_LIMIT -> FailurePresentation(stringResource(Res.string.error_rate_limited), Icons.Default.HourglassTop)
    RefreshFailureKind.AUTHENTICATION -> FailurePresentation(stringResource(Res.string.s_ab0a60e784), Icons.Default.Lock)
    RefreshFailureKind.REMOTE -> FailurePresentation(stringResource(Res.string.error_service), Icons.Default.SyncProblem)
    RefreshFailureKind.UNKNOWN, null -> FailurePresentation(stringResource(Res.string.error_unknown), Icons.Default.WarningAmber)
}
