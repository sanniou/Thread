package ai.saniou.coreui.widgets

import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.state.AppErrorType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ThreadStatusTone {
    Neutral,
    Warning,
    Error,
}

@Composable
fun ThreadStatusBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: ThreadStatusTone = ThreadStatusTone.Neutral,
    icon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
    details: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = when (tone) {
        ThreadStatusTone.Neutral -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary,
        )
        ThreadStatusTone.Warning -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
        )
        ThreadStatusTone.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.error,
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.first,
        contentColor = colors.second,
        border = BorderStroke(1.dp, colors.third.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon ?: when (tone) {
                        ThreadStatusTone.Neutral -> Icons.Default.Info
                        ThreadStatusTone.Warning -> Icons.Default.SyncProblem
                        ThreadStatusTone.Error -> Icons.Default.WarningAmber
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.third,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(message, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions,
                )
            }
            details()
        }
    }
}

@Composable
fun ThreadLoadingState(
    modifier: Modifier = Modifier,
    title: String = "正在准备内容",
    message: String = "Thread 正在同步来源与本地缓存。",
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(42.dp), strokeWidth = 3.dp)
        Text(
            title,
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            message,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ThreadErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    val presentation = error.presentation()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ModernEmptyState(
            icon = presentation.icon,
            title = presentation.title,
            description = error.message,
            action = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaniouButton(
                        onClick = onRetry,
                        text = presentation.action,
                    )
                    action()
                }
            },
        )
    }
}

private data class ErrorPresentation(
    val title: String,
    val action: String,
    val icon: ImageVector,
)

private fun AppError.presentation(): ErrorPresentation = when (type) {
    AppErrorType.NETWORK -> ErrorPresentation("当前处于离线状态", "重新连接", Icons.Default.CloudOff)
    AppErrorType.AUTHENTICATION -> ErrorPresentation("登录状态需要更新", "重新尝试", Icons.Default.Lock)
    AppErrorType.RATE_LIMIT -> ErrorPresentation("来源请求暂时受限", "稍后重试", Icons.Default.HourglassTop)
    AppErrorType.SERVER -> ErrorPresentation("来源服务暂不可用", "重新加载", Icons.Default.SyncProblem)
    AppErrorType.UNKNOWN -> ErrorPresentation("内容暂时无法加载", "重试", Icons.Default.WarningAmber)
}
