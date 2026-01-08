package ai.saniou.forum.ui.components

import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.theme.Dimens
import ai.saniou.forum.workflow.topicdetail.ThreadReply
import ai.saniou.thread.domain.model.forum.Comment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SubCommentsDialog(
    wrapper: UiStateWrapper<List<Comment>>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(Dimens.padding_medium)) {
                Text(
                    text = "楼中楼",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = Dimens.padding_medium)
                )

                StateLayout(
                    state = wrapper,
                    onRetry = onRetry
                ) { comments ->
                    LazyColumn {
                        items(comments) { comment ->
                            ThreadReply(
                                reply = comment,
                                poUserHash = "", // Sub-comments usually don't highlight PO in the same way or we need to pass it down
                                onReplyClicked = { /* No-op for sub-comments of sub-comments for now */ },
                                refClick = { /* Handle ref click if needed */ },
                                onImageClick = { /* Handle image click */ },
                                onCopy = { /* Handle copy */ },
                                onBookmark = { /* Handle bookmark */ },
                                onBookmarkImage = { /* Handle bookmark image */ },
                                onUserClick = { /* Handle user click */ }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
