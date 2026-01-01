package ai.saniou.coreui.widgets

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.reader.Article
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ArticleItem(
    article: Article,
    sourceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showUnreadIndicator: Boolean = true
) {
    val isRead = article.isRead
    val titleColor =
        if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = fontWeight,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                if (article.content.isNotBlank()) {
                    RichText(
                        text = article.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!article.author.isNullOrBlank()) {
                        Text(" · ", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = article.author!!,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = article.publishDate.toRelativeTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        leadingContent = {
            if (showUnreadIndicator && !isRead) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = "未读",
                    modifier = Modifier.size(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}