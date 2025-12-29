package ai.saniou.forum.workflow.topic.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.forum.workflow.home.StylizedForumItem
import ai.saniou.forum.workflow.home.SubCategoryBoxItem
import ai.saniou.thread.domain.model.forum.Channel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

    var isExpanded by remember { mutableStateOf(false) }
    val showExpandButton = subForums.size > 8
    val displayItems = if (isExpanded || !showExpandButton) subForums else subForums.take(8)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.padding_medium, vertical = Dimens.padding_small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "子板块",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (showExpandButton) {
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(text = if (isExpanded) "收起" else "查看更多")
                }
            }
        }

        if (listViewStyle == "boxes") {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.padding_medium, vertical = Dimens.padding_small),
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
                verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                displayItems.forEach { child ->
                    SubCategoryBoxItem(
                        forum = child,
                        onClick = { onForumClick(child) }
                    )
                }
            }
        } else {
            // Default list style
            Column(modifier = Modifier.padding(horizontal = Dimens.padding_small)) {
                displayItems.forEach { child ->
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
}