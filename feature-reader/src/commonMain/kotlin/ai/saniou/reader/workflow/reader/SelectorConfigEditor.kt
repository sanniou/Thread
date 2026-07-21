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
import org.jetbrains.compose.resources.stringResource
import thread.feature_reader.generated.resources.Res
import thread.feature_reader.generated.resources.s_d79148c66d
import thread.feature_reader.generated.resources.s_069758a2ae
import thread.feature_reader.generated.resources.s_1bd97802ac
import thread.feature_reader.generated.resources.s_2460b6d469
import thread.feature_reader.generated.resources.s_5807cf82a0
import thread.feature_reader.generated.resources.s_661f326eb8
import thread.feature_reader.generated.resources.s_7516587bb9
import thread.feature_reader.generated.resources.s_8a2604624b
import thread.feature_reader.generated.resources.s_aaee9a97a8
import thread.feature_reader.generated.resources.s_cbcd8508e9
import thread.feature_reader.generated.resources.s_d3d10c87f2
import thread.feature_reader.generated.resources.s_e153588a31
import thread.feature_reader.generated.resources.s_f12cf82a01

@Composable
private fun fieldDescription(field: String): String? = when (field) {
    // HTML
    "container" -> stringResource(Res.string.s_8a2604624b)
    "item" -> stringResource(Res.string.s_1bd97802ac)
    "title" -> stringResource(Res.string.s_f12cf82a01)
    "link" -> stringResource(Res.string.s_cbcd8508e9)
    "content" -> stringResource(Res.string.s_2460b6d469)
    // JSON
    "itemsPath" -> stringResource(Res.string.s_aaee9a97a8)
    "idPath" -> stringResource(Res.string.s_d3d10c87f2)
    "titlePath" -> stringResource(Res.string.s_661f326eb8)
    "linkPath" -> stringResource(Res.string.s_e153588a31)
    "contentPath" -> stringResource(Res.string.s_7516587bb9)
    "authorPath" -> stringResource(Res.string.s_5807cf82a0)
    "imagePath" -> stringResource(Res.string.s_069758a2ae)
    else -> null
}

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
                    description = fieldDescription(field),
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
                            contentDescription = stringResource(Res.string.s_d79148c66d),
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