package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.FeedSource
import ai.saniou.thread.domain.model.FeedType
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun FeedSourceDialog(
    source: FeedSource? = null,
    onDismiss: () -> Unit,
    onConfirm: (FeedSource) -> Unit
) {
    var name by remember { mutableStateOf(source?.name ?: "") }
    var url by remember { mutableStateOf(source?.url ?: "") }
    var type by remember { mutableStateOf(source?.type ?: FeedType.RSS) }
    var description by remember { mutableStateOf(source?.description ?: "") }
    var iconUrl by remember { mutableStateOf(source?.iconUrl ?: "") }

    // HTML-specific selectors
    var containerSelector by remember { mutableStateOf(source?.selectorConfig?.get("container") ?: "") }
    var itemSelector by remember { mutableStateOf(source?.selectorConfig?.get("item") ?: "") }
    var titleSelector by remember { mutableStateOf(source?.selectorConfig?.get("title") ?: "") }
    var linkSelector by remember { mutableStateOf(source?.selectorConfig?.get("link") ?: "") }
    var contentSelector by remember { mutableStateOf(source?.selectorConfig?.get("content") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (source == null) "添加订阅源" else "编辑订阅源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") }
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") }
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        FeedType.values().forEach { feedType ->
                            DropdownMenuItem(
                                text = { Text(feedType.name) },
                                onClick = {
                                    type = feedType
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (type == FeedType.HTML) {
                    Text("HTML 选择器", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    OutlinedTextField(value = containerSelector, onValueChange = { containerSelector = it }, label = { Text("容器 (Container)") })
                    OutlinedTextField(value = itemSelector, onValueChange = { itemSelector = it }, label = { Text("条目 (Item)") })
                    OutlinedTextField(value = titleSelector, onValueChange = { titleSelector = it }, label = { Text("标题 (Title)") })
                    OutlinedTextField(value = linkSelector, onValueChange = { linkSelector = it }, label = { Text("链接 (Link)") })
                    OutlinedTextField(value = contentSelector, onValueChange = { contentSelector = it }, label = { Text("内容 (Content)") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectorConfig = if (type == FeedType.HTML) {
                        mapOf(
                            "container" to containerSelector,
                            "item" to itemSelector,
                            "title" to titleSelector,
                            "link" to linkSelector,
                            "content" to contentSelector
                        )
                    } else {
                        emptyMap()
                    }

                    val newSource = FeedSource(
                        id = source?.id ?: Uuid.random().toString(),
                        name = name,
                        url = url,
                        type = type,
                        description = description.takeIf { it.isNotBlank() },
                        iconUrl = iconUrl.takeIf { it.isNotBlank() },
                        selectorConfig = selectorConfig
                    )
                    onConfirm(newSource)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}