package ai.saniou.reader.workflow.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
//        windowInsets = WindowInsets.ime // 键盘避让
    ) {
        Box(modifier = Modifier.padding(bottom = 16.dp)) {
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
                    Text("Interactive select not implemented yet", modifier = Modifier.padding(16.dp))
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
}

@Composable
private fun Step_Analyzing(url: String) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("正在分析页面...", style = MaterialTheme.typography.titleLarge)
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "分析失败",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = url,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "发现多个订阅源",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(sources) { source ->
                ListItem(
                    headlineContent = { Text(source.name.ifBlank { "未命名源" }) },
                    supportingContent = { Text(source.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.clickable { onSourceSelected(source) },
                    leadingContent = {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )
                HorizontalDivider()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
        modifier = Modifier.padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (source.id.isBlank()) "确认订阅信息" else "编辑订阅源",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // --- 基本信息 ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        ai.saniou.thread.domain.model.FeedType.entries.forEach { feedType ->
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("高级解析规则") },
                            supportingContent = { Text("配置如何提取文章内容") },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    modifier = Modifier.padding(8.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.transparent),
                            modifier = Modifier.clickable { isAdvancedConfigExpanded = !isAdvancedConfigExpanded }
                        )
                        AnimatedVisibility(visible = isAdvancedConfigExpanded) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBack) {
                    Text("上一步")
                }
                Spacer(modifier = Modifier.width(8.dp))
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
    // 简单的 URL 格式验证
    val isUrlValid = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("添加新订阅", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("输入网址 (RSS, HTML, JSON)") },
            placeholder = { Text("https://example.com/feed") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = url.isNotBlank() && !isUrlValid,
            supportingText = {
                if (url.isNotBlank() && !isUrlValid) {
                    Text("请输入以 http:// 或 https:// 开头的有效网址")
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onNext(url) },
                enabled = isUrlValid
            ) {
                Text("下一步")
            }
        }
    }
}
