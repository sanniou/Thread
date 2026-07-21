package ai.saniou.forum.workflow.source

import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.forum.workflow.source.SourceManagerContract.Event
import ai.saniou.thread.domain.model.source.SourceDescriptor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class SourceManagerPage : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel<SourceManagerViewModel>()
        val state by viewModel.state.collectAsState()
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SourceManagerContract.Effect.Message -> snackbar.showSnackbar(effect.value)
                }
            }
        }

        ThreadDetailScaffold(
            title = "内容来源",
            eyebrow = "FORUM SOURCES",
            subtitle = "管理内置连接器和动态 Discourse 实例",
            onBack = navigator::pop,
            snackbarHost = { SnackbarHost(snackbar) },
            actions = {
                IconButton(onClick = { viewModel.onEvent(Event.AddDiscourse) }) {
                    Icon(Icons.Default.Add, contentDescription = "添加 Discourse")
                }
            },
        ) { padding ->
            if (state.isLoading) {
                Column(
                    Modifier.padding(padding).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
            } else {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                            vertical = 20.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            ContextHero(
                                icon = Icons.Default.Hub,
                                title = "开放来源目录",
                                subtitle = "内置来源可启停；动态实例保存后立即进入运行目录",
                                metric = "${state.descriptors.count { it.enabled }} / ${state.descriptors.size} 已启用",
                            )
                        }
                        items(state.descriptors, key = SourceDescriptor::id) { descriptor ->
                            SourceDescriptorCard(
                                descriptor = descriptor,
                                enabled = !state.isSaving,
                                onToggle = { viewModel.onEvent(Event.SetEnabled(descriptor.id, it)) },
                                onEdit = { viewModel.onEvent(Event.Edit(descriptor)) },
                                onDelete = { viewModel.onEvent(Event.Remove(descriptor.id)) },
                            )
                        }
                    }
                }
            }
        }

        if (state.showEditor) {
            DiscourseEditorDialog(
                descriptor = state.editing,
                isSaving = state.isSaving,
                onDismiss = { viewModel.onEvent(Event.DismissEditor) },
                onSave = { id, name, url, key ->
                    viewModel.onEvent(Event.SaveDiscourse(id, name, url, key))
                },
            )
        }
    }
}

@Composable
private fun SourceDescriptorCard(
    descriptor: SourceDescriptor,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ThreadCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(descriptor.displayName, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (descriptor.isBuiltIn) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                    ) {
                        Text(
                            if (descriptor.isBuiltIn) "内置" else "动态",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text(
                    "${descriptor.type.value} · ${descriptor.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                descriptor.baseUrl?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!descriptor.isBuiltIn) {
                IconButton(onClick = onEdit, enabled = enabled) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete, enabled = enabled) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = descriptor.enabled, onCheckedChange = onToggle, enabled = enabled)
        }
    }
}

@Composable
private fun DiscourseEditorDialog(
    descriptor: SourceDescriptor?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var id by remember(descriptor) { mutableStateOf(descriptor?.id.orEmpty()) }
    var name by remember(descriptor) { mutableStateOf(descriptor?.displayName.orEmpty()) }
    var url by remember(descriptor) { mutableStateOf(descriptor?.baseUrl.orEmpty()) }
    var apiKey by remember(descriptor) {
        mutableStateOf(descriptor?.options?.get("developmentApiKey").orEmpty())
    }
    AdaptiveModal(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (descriptor == null) "添加 Discourse" else "编辑 Discourse",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "连接一个兼容 Discourse API 的社区实例。保存后将作为完整论坛来源参与导航、搜索和阅读。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                enabled = descriptor == null,
                label = { Text("实例 ID") },
                placeholder = { Text("例如 tech_forum") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("显示名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://forum.example.com/") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("开发测试 API Key（可选）") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                SaniouTextButton(onClick = onDismiss, text = "取消")
                SaniouButton(
                    onClick = { onSave(id, name, url, apiKey) },
                    enabled = !isSaving && id.isNotBlank() && name.isNotBlank() && url.isNotBlank(),
                    loading = isSaving,
                    text = "保存",
                )
            }
        }
    }
}
