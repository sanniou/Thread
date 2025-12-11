package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.FeedType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val fieldDescriptions = mapOf(
    // HTML
    "container" to "包含所有文章的列表选择器, e.g., #posts",
    "item" to "单个文章条目的选择器, e.g., .post-item",
    "title" to "文章标题的选择器, e.g., h2.entry-title",
    "link" to "文章链接的选择器, e.g., a.read-more",
    "content" to "文章内容的简短描述选择器, e.g., .post-excerpt",
    // JSON
    "itemsPath" to "指向文章数组的 JSON 路径, e.g., data.articles",
    "idPath" to "文章唯一 ID 的路径, e.g., id",
    "titlePath" to "文章标题的路径, e.g., title.rendered",
    "linkPath" to "文章链接的路径, e.g., link",
    "contentPath" to "文章内容的路径, e.g., content.rendered",
    "authorPath" to "作者信息的路径, e.g., author.name",
    "imagePath" to "文章图片的路径, e.g., featured_media.source_url"
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fields.forEach { field ->
                var value by remember(type, field, config[field]) { mutableStateOf(config[field] ?: "") }
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        val newConfig = config.toMutableMap()
                        newConfig[field] = it
                        onConfigChange(newConfig)
                    },
                    label = { Text(field) },
                    supportingText = {
                        fieldDescriptions[field]?.let { Text(it) }
                    }
                )
            }
        }
    }
}