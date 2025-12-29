package ai.saniou.forum.workflow.topic.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.forum.workflow.home.StylizedForumItem
import ai.saniou.forum.workflow.home.SubCategoryBoxItem
import ai.saniou.thread.domain.model.forum.Channel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubForumList(
    subForums: List<Channel>,
    listViewStyle: String?,
    onForumClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (subForums.isEmpty()) return

    if (listViewStyle == "boxes") {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.padding_standard, vertical = Dimens.padding_small),
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            subForums.forEach { child ->
                SubCategoryBoxItem(
                    forum = child,
                    onClick = { onForumClick(child) }
                )
            }
        }
    } else {
        // Default list style
        Column(modifier = modifier.padding(horizontal = Dimens.padding_small)) {
            subForums.forEach { child ->
                StylizedForumItem(
                    forum = child,
                    isSelected = false,
                    isFavorite = false, // TODO: Check favorite status
                    onForumClick = onForumClick,
                    onFavoriteToggle = {
                        // TODO: Implement favorite toggle
                    }
                )
            }
        }
    }
}