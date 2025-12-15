package ai.saniou.coreui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerItem(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconUrl: String? = null,
    badgeText: String? = null,
    isLoading: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        } else if (iconUrl != null) {
             NetworkImage(
                imageUrl = iconUrl,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else if (badgeText != null) {
            Badge { Text(badgeText) }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}
