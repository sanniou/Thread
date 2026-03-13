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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(Dimens.corner_radius_medium)
            )
            .padding(Dimens.padding_small),
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
    ) {
        subComments.forEach { comment ->
            SubCommentItem(
                comment = comment,
                onClick = { onCommentClick(comment) }
            )
        }

        if (totalCount > subComments.size) {
            Spacer(modifier = Modifier.height(Dimens.padding_tiny))
            Text(
                text = stringResource(Res.string.view_more_replies, totalCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onViewMoreClick)
                    .padding(vertical = Dimens.padding_tiny)
            )
        }
    }
}

@Composable
private fun SubCommentItem(
    comment: Comment,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.padding_tiny / 2)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    append(comment.author.name)
                    if (comment.isPo) {
                        append(" PO")
                    }
                    append(": ")
                }
                withStyle(
                    style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    append(comment.content.replace("\n", " "))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
