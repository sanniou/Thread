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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SUB-CHANNELS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Dimens.padding_standard, top = Dimens.padding_medium, bottom = Dimens.padding_small)
        )

        // Horizontal Scrolling Chip Group style for better space efficiency
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimens.padding_standard),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subForums.size) { index ->
                val child = subForums[index]
                androidx.compose.material3.FilterChip(
                    selected = false,
                    onClick = { onForumClick(child) },
                    label = { Text(child.name) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null
                )
            }
        }
    }
}