package ai.saniou.coreui.widgets

import org.jetbrains.compose.resources.stringResource
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
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.s_2bd11d56d9
import thread.core_ui.generated.resources.s_43341a1c24
import thread.core_ui.generated.resources.s_5982c44c18
import thread.core_ui.generated.resources.s_6513bc3959
import thread.core_ui.generated.resources.s_6f33e1a69a
import thread.core_ui.generated.resources.s_74d54da6e7
import thread.core_ui.generated.resources.s_a980ac855c
import thread.core_ui.generated.resources.s_af2fab7689
import thread.core_ui.generated.resources.s_b8e0d519a4
import thread.core_ui.generated.resources.s_bac95e1e52
import thread.core_ui.generated.resources.action_retry
import thread.core_ui.generated.resources.s_edb8f0043f
import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.state.AppErrorType
import ai.saniou.coreui.state.localizedMessage

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
    title: String = stringResource(Res.string.s_43341a1c24),
    message: String = stringResource(Res.string.s_edb8f0043f),
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            title,
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
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
            description = error.localizedMessage(),
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

@Composable
private fun AppError.presentation(): ErrorPresentation = when (type) {
    AppErrorType.NETWORK -> ErrorPresentation(stringResource(Res.string.s_6f33e1a69a), stringResource(Res.string.s_bac95e1e52), Icons.Default.CloudOff)
    AppErrorType.AUTHENTICATION -> ErrorPresentation(stringResource(Res.string.s_74d54da6e7), stringResource(Res.string.s_a980ac855c), Icons.Default.Lock)
    AppErrorType.RATE_LIMIT -> ErrorPresentation(stringResource(Res.string.s_2bd11d56d9), stringResource(Res.string.s_b8e0d519a4), Icons.Default.HourglassTop)
    AppErrorType.SERVER -> ErrorPresentation(stringResource(Res.string.s_6513bc3959), stringResource(Res.string.s_5982c44c18), Icons.Default.SyncProblem)
    AppErrorType.UNKNOWN -> ErrorPresentation(stringResource(Res.string.s_af2fab7689), stringResource(Res.string.action_retry), Icons.Default.WarningAmber)
}
