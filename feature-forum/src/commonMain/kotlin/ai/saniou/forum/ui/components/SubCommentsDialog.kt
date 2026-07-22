package ai.saniou.forum.ui.components

import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.forum.workflow.topicdetail.ThreadReply
import ai.saniou.thread.domain.model.forum.Comment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.load_more
import thread.feature_forum.generated.resources.sub_comments_title

@Composable
fun SubCommentsSheet(
    wrapper: UiStateWrapper<List<Comment>>,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AdaptiveModal(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp, max = 720.dp)
                .padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.sub_comments_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(horizontal = Dimens.padding_standard)
                    .padding(bottom = Dimens.padding_medium)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            StateLayout(
                state = wrapper,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            ) { comments ->
                LazyColumn {
                    items(comments, key = { it.id }) { comment ->
                        ThreadReply(
                            reply = comment,
                            poUserHash = "", // Sub-comments usually don't highlight PO in the same way
                            onReplyClicked = { /* No-op for sub-comments of sub-comments */ },
                            refClick = { /* Handle ref click if needed */ },
                            onImageClick = { /* Handle image click */ },
                            onCopy = { /* Handle copy */ },
                            onBookmark = { /* Handle bookmark */ },
                            onBookmarkImage = { /* Handle bookmark image */ },
                            onUserClick = { /* Handle user click */ },
                            modifier = threadAnimateItem(),
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = Dimens.padding_standard)
                        )
                    }
                    if (hasMore || isLoadingMore) {
                        item(key = "sub-load-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.padding_standard),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator()
                                } else {
                                    SaniouTextButton(
                                        onClick = onLoadMore,
                                        text = stringResource(Res.string.load_more),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
