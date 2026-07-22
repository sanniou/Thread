package ai.saniou.forum.ui.components

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.thread.domain.model.forum.Comment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.label_anonymous

@Composable
fun RecentReplies(replies: List<Comment>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.padding_small),
    ) {
        replies.forEach { reply ->
            ReplyItem(reply)
        }
    }
}

@Composable
private fun ReplyItem(reply: Comment) {
    val anonymous = stringResource(Res.string.label_anonymous)
    val name = reply.author.name.takeIf { it.isNotBlank() && it != anonymous } ?: reply.author.id
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = reply.createdAt.toRelativeTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ForumRichText(
            text = reply.content,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            blankLinePolicy = BlankLinePolicy.REMOVE,
        )
    }
}
