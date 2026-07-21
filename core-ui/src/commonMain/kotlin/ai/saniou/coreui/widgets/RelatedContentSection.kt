package ai.saniou.coreui.widgets

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.content.ContentRelationKind
import ai.saniou.thread.domain.model.content.RelatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.s_40a1549708
import thread.core_ui.generated.resources.s_682836a89a

/** Compact graph projection designed to live inside both forum and Reader detail canvases. */
@Composable
fun RelatedContentSection(
    items: LazyPagingItems<RelatedContent>,
    onOpen: (RelatedContent) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 8,
) {
    if (items.itemCount == 0) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(Res.string.s_682836a89a), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            stringResource(Res.string.s_40a1549708),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(minOf(items.itemCount, maxItems)) { index ->
            val related = items[index] ?: return@repeat
            ThreadCard(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(related) },
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        related.relation.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        related.publishedAt.toRelativeTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    related.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                related.summary.takeIf(String::isNotBlank)?.let { summary ->
                    Text(
                        summary.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun ContentRelationKind.label() = when (this) {
    ContentRelationKind.REPLY_TO -> "回复链"
    ContentRelationKind.REFERENCES -> "正文引用"
    ContentRelationKind.REPOST_OF -> "转发来源"
    ContentRelationKind.SAME_AUTHOR -> "同一作者"
    ContentRelationKind.SAME_TAG -> "相同标签"
    ContentRelationKind.CANONICAL_DUPLICATE -> "同一内容"
}
