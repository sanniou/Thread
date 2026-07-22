package ai.saniou.reader.workflow.reader

import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.thread.domain.model.reader.FeedType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_reader.generated.resources.Res
import thread.feature_reader.generated.resources.s_0c8b3594f5
import thread.feature_reader.generated.resources.action_back
import thread.feature_reader.generated.resources.s_127db1eb5e
import thread.feature_reader.generated.resources.s_1be7ae4fc2
import thread.feature_reader.generated.resources.s_22735a939e
import thread.feature_reader.generated.resources.s_251d583130
import thread.feature_reader.generated.resources.s_293a341e88
import thread.feature_reader.generated.resources.s_297a7ec438
import thread.feature_reader.generated.resources.s_315d27498f
import thread.feature_reader.generated.resources.s_3a9a579a96
import thread.feature_reader.generated.resources.s_3f5cb2d1a1
import thread.feature_reader.generated.resources.s_45a2727884
import thread.feature_reader.generated.resources.s_4b62c5ce6c
import thread.feature_reader.generated.resources.action_cancel
import thread.feature_reader.generated.resources.s_57937ccefc
import thread.feature_reader.generated.resources.s_75ef1241c0
import thread.feature_reader.generated.resources.s_899dfaf555
import thread.feature_reader.generated.resources.s_8e17667027
import thread.feature_reader.generated.resources.s_99250ff7cc
import thread.feature_reader.generated.resources.s_b37255ca3a
import thread.feature_reader.generated.resources.s_bb77d79355
import thread.feature_reader.generated.resources.s_bea40ee383
import thread.feature_reader.generated.resources.s_c02d95def4
import thread.feature_reader.generated.resources.s_c3147ba167
import thread.feature_reader.generated.resources.s_c9e72455c7
import thread.feature_reader.generated.resources.s_dee3a22068
import thread.feature_reader.generated.resources.action_retry
import thread.feature_reader.generated.resources.s_e4e46c7235
import thread.feature_reader.generated.resources.s_ea0ef2ae72
import thread.feature_reader.generated.resources.action_save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedSourceSheet(
    viewModel: AddFeedSourceViewModel,
    onDismiss: () -> Unit,
    onSave: (ai.saniou.thread.domain.model.reader.FeedSource) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    AdaptiveModal(onDismissRequest = onDismiss) {
        Box {
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
        Text(stringResource(Res.string.s_899dfaf555), style = MaterialTheme.typography.titleLarge)
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
            text = stringResource(Res.string.s_127db1eb5e),
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
            SaniouTextButton(onClick = onBack, text = stringResource(Res.string.action_back))
            Spacer(modifier = Modifier.width(8.dp))
            SaniouButton(onClick = onRetry, text = stringResource(Res.string.action_retry))
        }
    }
}

@Composable
private fun Step_SelectSource(
    sources: List<ai.saniou.thread.domain.model.reader.FeedSource>,
    onSourceSelected: (ai.saniou.thread.domain.model.reader.FeedSource) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(Res.string.s_bea40ee383),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(sources) { source ->
                ListItem(
                    headlineContent = { Text(source.name.ifBlank { stringResource(Res.string.s_dee3a22068) }) },
                    supportingContent = { Text(source.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = threadAnimateItem().clickable { onSourceSelected(source) },
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
            SaniouTextButton(onClick = onBack, text = stringResource(Res.string.action_back))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step_ConfirmSource(
    source: ai.saniou.thread.domain.model.reader.FeedSource,
    onConfirm: (ai.saniou.thread.domain.model.reader.FeedSource) -> Unit,
    onBack: () -> Unit
) {
    var name by remember(source.name) { mutableStateOf(source.name) }
    var url by remember(source.url) { mutableStateOf(source.url) }
    var type by remember(source.type) { mutableStateOf(source.type) }
    var config by remember(source.selectorConfig) { mutableStateOf(source.selectorConfig) }
    var autoRefresh by remember(source.autoRefresh) { mutableStateOf(source.autoRefresh) }
    var refreshInterval by remember(source.refreshInterval) { mutableStateOf(source.refreshInterval) }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isIntervalMenuExpanded by remember { mutableStateOf(false) }
    var isAdvancedConfigExpanded by remember { mutableStateOf(false) }

    val intervalOptions = listOf(
        900000L to stringResource(Res.string.s_c02d95def4),
        1800000L to stringResource(Res.string.s_c9e72455c7),
        3600000L to stringResource(Res.string.s_3a9a579a96),
        7200000L to stringResource(Res.string.s_b37255ca3a),
        21600000L to stringResource(Res.string.s_57937ccefc),
        43200000L to stringResource(Res.string.s_293a341e88),
        86400000L to stringResource(Res.string.s_45a2727884)
    )

    LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (source.id.isBlank()) stringResource(Res.string.s_0c8b3594f5) else stringResource(Res.string.s_bb77d79355),
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
                    label = { Text(stringResource(Res.string.s_1be7ae4fc2)) },
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
                        label = { Text(stringResource(Res.string.s_e4e46c7235)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isTypeMenuExpanded,
                        onDismissRequest = { isTypeMenuExpanded = false }
                    ) {
                        FeedType.entries.forEach { feedType ->
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

        // --- 刷新设置 ---
        item {
            Text(
                stringResource(Res.string.s_c3147ba167),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.s_297a7ec438))
                    Switch(
                        checked = autoRefresh,
                        onCheckedChange = { autoRefresh = it }
                    )
                }

                AnimatedVisibility(visible = autoRefresh) {
                    ExposedDropdownMenuBox(
                        expanded = isIntervalMenuExpanded,
                        onExpandedChange = { isIntervalMenuExpanded = !isIntervalMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = intervalOptions.find { it.first == refreshInterval }?.second ?: stringResource(Res.string.s_315d27498f, refreshInterval / 60000),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.s_3f5cb2d1a1)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isIntervalMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isIntervalMenuExpanded,
                            onDismissRequest = { isIntervalMenuExpanded = false }
                        ) {
                            intervalOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        refreshInterval = value
                                        isIntervalMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 高级配置 (可折叠, 仅在 HTML/JSON 时显示) ---
        if (type == FeedType.HTML || type == FeedType.JSON) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(Res.string.s_4b62c5ce6c)) },
                            supportingContent = { Text(stringResource(Res.string.s_8e17667027)) },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    modifier = Modifier.padding(8.dp)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                SaniouTextButton(onClick = onBack, text = stringResource(Res.string.s_75ef1241c0))
                Spacer(modifier = Modifier.width(8.dp))
                SaniouButton(
                    onClick = {
                        onConfirm(source.copy(
                            name = name,
                            url = url,
                            type = type,
                            selectorConfig = config,
                            autoRefresh = autoRefresh,
                            refreshInterval = refreshInterval
                        ))
                    },
                    text = stringResource(Res.string.action_save),
                )
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
        Text(stringResource(Res.string.s_22735a939e), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(Res.string.s_99250ff7cc)) },
            placeholder = { Text("https://example.com/feed") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = url.isNotBlank() && !isUrlValid,
            supportingText = {
                if (url.isNotBlank() && !isUrlValid) {
                    Text(stringResource(Res.string.s_251d583130))
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SaniouTextButton(onClick = onDismiss, text = stringResource(Res.string.action_cancel))
            Spacer(modifier = Modifier.width(8.dp))
            SaniouButton(
                onClick = { onNext(url) },
                enabled = isUrlValid,
                text = stringResource(Res.string.s_ea0ef2ae72),
            )
        }
    }
}
