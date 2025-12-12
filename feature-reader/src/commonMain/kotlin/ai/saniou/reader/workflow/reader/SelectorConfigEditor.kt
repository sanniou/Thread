package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.reader.FeedType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val fieldDescriptions = mapOf(
    // HTML
    "container" to "包含所有文章的列表选择器\n例如: #posts 或 .article-list",
    "item" to "单个文章条目的选择器\n例如: .post-item 或 article",
    "title" to "文章标题的选择器\n例如: h2.entry-title 或 .title > a",
    "link" to "文章链接的选择器\n例如: a.read-more 或 a[href]",
    "content" to "文章内容的简短描述选择器\n例如: .post-excerpt 或 .summary",
    // JSON
    "itemsPath" to "指向文章数组的 JSON 路径\n例如: data.articles 或 items",
    "idPath" to "文章唯一 ID 的路径\n例如: id 或 uuid",
    "titlePath" to "文章标题的路径\n例如: title.rendered 或 headline",
    "linkPath" to "文章链接的路径\n例如: link 或 url",
    "contentPath" to "文章内容的路径\n例如: content.rendered 或 body",
    "authorPath" to "作者信息的路径\n例如: author.name",
    "imagePath" to "文章图片的路径\n例如: featured_media.source_url"
)

@Composable
fun SelectorConfigEditor(
    type: FeedType,
    config: Map<String, String>,
    onConfigChange: (Map<String, String>) -> Unit
) {
    val fields = when (type) {
        FeedType.HTML -> listOf("container", "item", "title", "link", "content")
        FeedType.JSON -> listOf("itemsPath", "idPath", "titlePath", "linkPath", "contentPath", "authorPath", "imagePath")
        else -> emptyList()
    }

    if (fields.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            fields.forEach { field ->
                ConfigField(
                    label = field,
                    value = config[field] ?: "",
                    description = fieldDescriptions[field],
                    onValueChange = { newValue ->
                        val newConfig = config.toMutableMap()
                        newConfig[field] = newValue
                        onConfigChange(newConfig)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    description: String?,
    onValueChange: (String) -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (description != null) {
                    IconButton(onClick = { showHelp = !showHelp }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "显示帮助",
                            tint = if (showHelp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        )
        
        AnimatedVisibility(visible = showHelp && description != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}