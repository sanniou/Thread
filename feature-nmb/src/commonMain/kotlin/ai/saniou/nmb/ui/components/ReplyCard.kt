package ai.saniou.nmb.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.RichText
import ai.saniou.nmb.data.entity.ThreadReply
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
            .clip(RoundedCornerShape(Dimens.padding_medium))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Dimens.padding_medium),
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
    ) {
        replies.forEach { reply ->
            ReplyItem(reply)
        }
    }
}


@Composable
private fun ReplyItem(reply: ThreadReply) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_extra_small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_extra_small)
        ) {
            Text(
                text = reply.userHash,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RichText(
            text = reply.content,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            blankLinePolicy = BlankLinePolicy.REMOVE
        )
    }
}
