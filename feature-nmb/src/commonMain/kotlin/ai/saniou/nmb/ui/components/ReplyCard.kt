package ai.saniou.nmb.ui.components

import ai.saniou.nmb.data.entity.Reply
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val PADDING_SMALL = 8.dp
private val PADDING_EXTRA_SMALL = 4.dp

@Composable
fun RecentReplies(replies: List<Reply>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PADDING_EXTRA_SMALL))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(PADDING_SMALL),
        verticalArrangement = Arrangement.spacedBy(PADDING_EXTRA_SMALL)
    ) {
        replies.forEachIndexed { index, reply ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            ReplyItem(reply)
        }
    }
}

@Composable
fun ReplyItem(reply: Reply) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PADDING_EXTRA_SMALL)
    ) {
        ThreadAuthor(reply)
        HtmlText(
            text = reply.content,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}