package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.FeedSource
import ai.saniou.thread.domain.model.FeedType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private class FeedSourceDialogState(source: FeedSource?) {
    var name by mutableStateOf(source?.name ?: "")
    var url by mutableStateOf(source?.url ?: "")
    var type by mutableStateOf(source?.type ?: FeedType.RSS)
    var description by mutableStateOf(source?.description ?: "")
    var iconUrl by mutableStateOf(source?.iconUrl ?: "")
    val selectorConfig = mutableStateOf(source?.selectorConfig?.toMutableMap() ?: mutableMapOf())

    @OptIn(ExperimentalUuidApi::class)
    fun toFeedSource(existingId: String?): FeedSource {
        return FeedSource(
            id = existingId ?: Uuid.random().toString(),
            name = name,
            url = url,
            type = type,
            description = description.takeIf { it.isNotBlank() },
            iconUrl = iconUrl.takeIf { it.isNotBlank() },
            selectorConfig = selectorConfig.value.filterValues { it.isNotBlank() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSourceDialog(
    source: FeedSource? = null,
    onDismiss: () -> Unit,
    onConfirm: (FeedSource) -> Unit
) {
    val state = remember { FeedSourceDialogState(source) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (source == null) "添加订阅源" else "编辑订阅源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state.name = it },
                    label = { Text("名称") }
                )
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { state.url = it },
                    label = { Text("URL") }
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.type.name,
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
                                    state.type = feedType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                SelectorConfigEditor(state.type, state.selectorConfig.value)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(state.toFeedSource(source?.id)) }) {
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

@Composable
private fun SelectorConfigEditor(type: FeedType, config: MutableMap<String, String>) {
    val fields = when (type) {
        FeedType.HTML -> listOf("container", "item", "title", "link", "content")
        FeedType.JSON -> listOf("itemsPath", "idPath", "titlePath", "linkPath", "contentPath", "authorPath", "imagePath")
        else -> emptyList()
    }

    if (fields.isNotEmpty()) {
        Text(
            text = "${type.name} 选择器",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        fields.forEach { field ->
            var value by remember(type, field) { mutableStateOf(config[field] ?: "") }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    config[field] = it
                },
                label = { Text(field) }
            )
        }
    }
}