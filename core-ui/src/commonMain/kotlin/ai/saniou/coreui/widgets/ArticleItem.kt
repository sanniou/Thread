package ai.saniou.coreui.widgets

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.layout.ThreadWindowWidthClass
import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.reader.Article
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.s_1e230aa201
import thread.core_ui.generated.resources.s_2d2cdabf29

@Composable
fun ArticleItem(
    article: Article,
    sourceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showUnreadIndicator: Boolean = true,
) {
    val windowInfo = LocalThreadWindowInfo.current
    val isRead = article.isRead
    val titleColor =
        if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface
    val fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold
    val thumbWidth = if (windowInfo.widthClass >= ThreadWindowWidthClass.Expanded) 148.dp else 132.dp
    val thumbHeight = if (windowInfo.widthClass >= ThreadWindowWidthClass.Expanded) 104.dp else 96.dp

    ThreadCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_standard),
        ) {
            if (showUnreadIndicator && !isRead) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = stringResource(Res.string.s_1e230aa201),
                    modifier = Modifier.padding(top = 7.dp).size(8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = fontWeight,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val summary = article.description.ifBlank { article.content }
                if (summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.padding_small))
                    RichText(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(Dimens.padding_medium))
                } else {
                    Spacer(modifier = Modifier.height(Dimens.padding_small))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!article.author.isNullOrBlank()) {
                        Text(" · ", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = article.author!!,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (article.isBookmarked) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = stringResource(Res.string.s_2d2cdabf29),
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(Dimens.padding_tiny + 2.dp))
                    }
                    Text(
                        text = article.publishDate.toRelativeTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (
                !article.imageUrl.isNullOrBlank() &&
                windowInfo.widthClass >= ThreadWindowWidthClass.Medium
            ) {
                NetworkImage(
                    imageUrl = article.imageUrl!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .clip(MaterialTheme.shapes.medium),
                )
            }
        }
    }
}
