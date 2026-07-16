package ai.saniou.thread.feature.search

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.search.GlobalSearchResult
import ai.saniou.thread.domain.model.search.GlobalSearchType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Instant

@Composable
fun GlobalSearchResults(
    results: List<GlobalSearchResult>,
    modifier: Modifier = Modifier,
    onOpen: (GlobalSearchResult) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(results, key = { "${it.type}:${it.sourceId}:${it.id}" }) { result ->
            GlobalSearchResultRow(result, onOpen)
        }
    }
}

@Composable
fun GlobalSearchResultRow(
    result: GlobalSearchResult,
    onOpen: (GlobalSearchResult) -> Unit,
) {
    val presentation = result.type.presentation()
    Surface(
        onClick = { onOpen(result) },
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${presentation.label}，${result.title}，来自 ${result.sourceName}"
        },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    presentation.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (result.snippet.isNotBlank()) {
                    Text(
                        result.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    buildString {
                        append(presentation.label).append(" · ").append(result.sourceName)
                        result.author?.takeIf(String::isNotBlank)?.let { append(" · ").append(it) }
                        append(" · ").append(Instant.fromEpochMilliseconds(result.publishedAtEpochMillis).toRelativeTimeString())
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

data class SearchTypePresentation(val label: String, val icon: ImageVector)

fun GlobalSearchType.presentation(): SearchTypePresentation = when (this) {
    GlobalSearchType.TOPIC -> SearchTypePresentation("主题", Icons.Default.Forum)
    GlobalSearchType.COMMENT -> SearchTypePresentation("回复", Icons.Default.QuestionAnswer)
    GlobalSearchType.ARTICLE -> SearchTypePresentation("文章", Icons.AutoMirrored.Filled.Article)
}
