package ai.saniou.forum.ui.components

import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.theme.Dimens
import ai.saniou.forum.workflow.topicdetail.ThreadReply
import ai.saniou.thread.domain.model.forum.Comment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.sub_comments_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCommentsSheet(
    wrapper: UiStateWrapper<List<Comment>>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f) // Occupy 75% of screen height
                .windowInsetsPadding(WindowInsets.navigationBars) // Handle safe area
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
                    items(comments) { comment ->
                        ThreadReply(
                            reply = comment,
                            poUserHash = "", // Sub-comments usually don't highlight PO in the same way
                            onReplyClicked = { /* No-op for sub-comments of sub-comments */ },
                            refClick = { /* Handle ref click if needed */ },
                            onImageClick = { /* Handle image click */ },
                            onCopy = { /* Handle copy */ },
                            onBookmark = { /* Handle bookmark */ },
                            onBookmarkImage = { /* Handle bookmark image */ },
                            onUserClick = { /* Handle user click */ }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = Dimens.padding_standard)
                        )
                    }
                }
            }
        }
    }
}