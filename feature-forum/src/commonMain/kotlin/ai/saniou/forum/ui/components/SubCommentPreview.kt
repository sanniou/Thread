package ai.saniou.forum.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.forum.Comment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.view_more_replies

/**
 * 楼中楼预览组件 (Twitter 风格)
 * 显示前 N 条评论，点击查看更多
 */
@Composable
fun SubCommentPreview(
    subComments: List<Comment>,
    totalCount: Int,
    onViewMoreClick: () -> Unit,
    onCommentClick: (Comment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (subComments.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.corner_radius_medium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .padding(Dimens.padding_small)
    ) {
        subComments.forEachIndexed { index, comment ->
            SubCommentItem(
                comment = comment,
                onClick = { onCommentClick(comment) }
            )
            if (index < subComments.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (totalCount > subComments.size) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.view_more_replies, totalCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onViewMoreClick)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SubCommentItem(
    comment: Comment,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    append(comment.author.name)
                    if (comment.isPo) {
                        append(" (PO)")
                    }
                    append(": ")
                }
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    append(comment.content.replace("\n", " "))
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}