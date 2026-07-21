package ai.saniou.coreui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class CacheStatusTone {
    CACHED,
    REFRESHING,
    STALE,
    OFFLINE,
}

/**
 * Compact cache/refresh status chip for list heroes and timeline tops.
 * Keeps product copy human: "缓存优先" not internal store names.
 */
@Composable
fun CacheStatusBanner(
    title: String,
    tone: CacheStatusTone,
    modifier: Modifier = Modifier,
    detail: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = when (tone) {
        CacheStatusTone.CACHED -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        CacheStatusTone.REFRESHING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        CacheStatusTone.STALE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        CacheStatusTone.OFFLINE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = when (tone) {
        CacheStatusTone.CACHED -> Icons.Outlined.Cached
        CacheStatusTone.REFRESHING -> Icons.Outlined.Sync
        CacheStatusTone.STALE -> Icons.Outlined.Cached
        CacheStatusTone.OFFLINE -> Icons.Outlined.CloudOff
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.first,
        contentColor = colors.second,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null)
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                detail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.second.copy(alpha = 0.86f),
                    )
                }
            }
            action?.invoke()
        }
    }
}
