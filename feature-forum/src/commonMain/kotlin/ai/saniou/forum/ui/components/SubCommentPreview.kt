package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.forum.Comment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.view_more_replies

@Composable
fun SubCommentPreview(
    subComments: List<Comment>,
    totalCount: Int,
    onViewMoreClick: () -> Unit,
    onCommentClick: (Comment) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (subComments.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
    ) {
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .height(((subComments.size.coerceAtMost(3) * 22) + 28).dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(onClick = onViewMoreClick)
                .padding(horizontal = Dimens.padding_medium, vertical = Dimens.padding_small),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
        ) {
            subComments.forEach { comment ->
                SubCommentItem(
                    comment = comment,
                    onClick = { onCommentClick(comment) },
                )
            }

            if (totalCount > subComments.size) {
                Text(
                    text = stringResource(Res.string.view_more_replies, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onViewMoreClick)
                        .padding(vertical = Dimens.padding_tiny),
                )
            }
        }
    }
}

@Composable
private fun SubCommentItem(
    comment: Comment,
    onClick: () -> Unit,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                val name = comment.author.name.ifBlank { comment.author.id }
                append(name)
                if (comment.isPo) {
                    append(" · PO")
                }
                append("  ")
            }
            withStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                append(comment.content.replace("\n", " ").trim())
            }
        },
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    )
}
