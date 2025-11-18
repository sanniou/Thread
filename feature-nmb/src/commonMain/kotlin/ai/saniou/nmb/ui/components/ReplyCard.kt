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
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.theme.Dimens

@Composable
fun RecentReplies(replies: List<Reply>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.padding_extra_small))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Dimens.padding_small),
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_extra_small)
    ) {
        replies.forEachIndexed { index, reply ->
            if (index > 0) {
                HorizontalDivider()
            }
            ReplyItem(reply)
        }
    }
}

@Composable
fun ReplyItem(reply: Reply) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_extra_small)
    ) {
        ThreadAuthor(reply)
        RichText(
            text = reply.content,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}