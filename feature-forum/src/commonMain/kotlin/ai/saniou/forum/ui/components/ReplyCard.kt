package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.RichText
import ai.saniou.thread.domain.model.forum.ThreadReply
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun RecentReplies(replies: List<ThreadReply>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Dimens.padding_medium),
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
    ) {
        replies.forEach { reply ->
            ReplyItem(reply)
        }
    }
}


@Composable
private fun ReplyItem(reply: ThreadReply) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)
        ) {
            Text(
                text = reply.userHash,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = reply.createdAt.toRelativeTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        RichText(
            text = reply.content,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            blankLinePolicy = BlankLinePolicy.REMOVE
        )
    }
}
