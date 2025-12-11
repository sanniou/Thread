package ai.saniou.reader.workflow.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedSourceSheet(
    viewModel: AddFeedSourceViewModel,
    onDismiss: () -> Unit,
    onSave: (ai.saniou.thread.domain.model.FeedSource) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // 根据 ViewModel 的状态显示不同的向导步骤
        when (val state = uiState) {
            is AddFeedSourceUiState.EnterUrl -> {
                Step1_EnterUrl(
                    onNext = { viewModel.onUrlEntered(it) },
                    onDismiss = onDismiss
                )
            }

            is AddFeedSourceUiState.Analyzing -> {
                Step_Analyzing(state.url)
            }

            is AddFeedSourceUiState.AnalysisFailed -> {
                Step_AnalysisFailed(
                    url = state.url,
                    error = state.error,
                    onRetry = { viewModel.onUrlEntered(state.url) },
                    onBack = { viewModel.onBack() }
                )
            }

            is AddFeedSourceUiState.SelectSource -> {
                Step_SelectSource(
                    sources = state.sources,
                    onSourceSelected = { viewModel.onSourceSelected(it) },
                    onBack = { viewModel.onBack() }
                )
            }

            is AddFeedSourceUiState.InteractiveSelect -> {
                // TODO: 实现交互式的网页选择器 UI
            }

            is AddFeedSourceUiState.ConfirmSource -> {
                Step_ConfirmSource(
                    source = state.source,
                    onConfirm = { source -> viewModel.onConfirm(source, onSuccess = { onSave(source) }) },
                    onBack = { viewModel.onBack() }
                )
            }
        }
    }
}

@Composable
private fun Step_Analyzing(url: String) {
    Column(
        modifier = Modifier.padding(32.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("正在分析...", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        Text(
            text = url,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Step_AnalysisFailed(
    url: String,
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("分析失败", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = error,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.error
        )
        Text(
            text = url,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onBack) {
                Text("返回")
            }
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun Step_SelectSource(
    sources: List<ai.saniou.thread.domain.model.FeedSource>,
    onSourceSelected: (ai.saniou.thread.domain.model.FeedSource) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "发现多个订阅源",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn {
            items(sources) { source ->
                ListItem(
                    headlineContent = { Text(source.name) },
                    supportingContent = { Text(source.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.clickable { onSourceSelected(source) }
                )
                Divider()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step_ConfirmSource(
    source: ai.saniou.thread.domain.model.FeedSource,
    onConfirm: (ai.saniou.thread.domain.model.FeedSource) -> Unit,
    onBack: () -> Unit
) {
    var name by remember(source.name) { mutableStateOf(source.name) }
    var url by remember(source.url) { mutableStateOf(source.url) }
    var type by remember(source.type) { mutableStateOf(source.type) }
    var config by remember(source.selectorConfig) { mutableStateOf(source.selectorConfig) }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isAdvancedConfigExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (source.id.isBlank()) "添加订阅源" else "编辑订阅源",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
        }

        // --- 基本信息 ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = isTypeMenuExpanded,
                    onExpandedChange = { isTypeMenuExpanded = !isTypeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isTypeMenuExpanded,
                        onDismissRequest = { isTypeMenuExpanded = false }
                    ) {
                        ai.saniou.thread.domain.model.FeedType.values().forEach { feedType ->
                            DropdownMenuItem(
                                text = { Text(feedType.name) },
                                onClick = {
                                    type = feedType
                                    isTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 高级配置 (可折叠, 仅在 HTML/JSON 时显示) ---
        if (type == ai.saniou.thread.domain.model.FeedType.HTML || type == ai.saniou.thread.domain.model.FeedType.JSON) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("高级配置") },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand"
                                )
                            },
                            modifier = Modifier.clickable { isAdvancedConfigExpanded = !isAdvancedConfigExpanded }
                        )
                        AnimatedVisibility(visible = isAdvancedConfigExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SelectorConfigEditor(
                                    type = type,
                                    config = config,
                                    onConfigChange = { config = it }
                                )
                            }
                        }
                    }
                }
            }
        }


        // --- 操作按钮 ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
                Button(onClick = {
                    onConfirm(source.copy(name = name, url = url, type = type, selectorConfig = config))
                }) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun Step1_EnterUrl(onNext: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("添加订阅源", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("输入订阅源网址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
            Button(onClick = { onNext(url) }) {
                Text("下一步")
            }
        }
    }
}
