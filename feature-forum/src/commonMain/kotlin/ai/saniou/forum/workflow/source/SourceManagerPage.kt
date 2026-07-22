package ai.saniou.forum.workflow.source

import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ThreadLoadingState
import ai.saniou.coreui.theme.threadAnimateItem
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
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.eyebrow_forum_sources
import thread.feature_forum.generated.resources.s_09ceea7644
import thread.feature_forum.generated.resources.s_3507ddb87f
import thread.feature_forum.generated.resources.action_delete
import thread.feature_forum.generated.resources.action_add_discourse
import thread.feature_forum.generated.resources.s_5fee7d843d
import thread.feature_forum.generated.resources.s_6780ab7294
import thread.feature_forum.generated.resources.label_content_sources
import thread.feature_forum.generated.resources.s_6fe30db879
import thread.feature_forum.generated.resources.label_activity
import thread.feature_forum.generated.resources.label_display_name
import thread.feature_forum.generated.resources.s_7bda6019d9
import thread.feature_forum.generated.resources.s_96d7780f27
import thread.feature_forum.generated.resources.action_edit
import thread.feature_forum.generated.resources.s_ac8895327b
import thread.feature_forum.generated.resources.s_acc77f9e5b
import thread.feature_forum.generated.resources.s_ae96a77def
import thread.feature_forum.generated.resources.s_b4e08a4b66
import thread.feature_forum.generated.resources.s_bb7f40922f
import thread.feature_forum.generated.resources.action_save
import thread.feature_forum.generated.resources.subscription_cancel

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
            title = stringResource(Res.string.label_content_sources),
            eyebrow = stringResource(Res.string.eyebrow_forum_sources),
            subtitle = stringResource(Res.string.s_acc77f9e5b),
            onBack = navigator::pop,
            snackbarHost = { SnackbarHost(snackbar) },
            actions = {
                IconButton(onClick = { viewModel.onEvent(Event.AddDiscourse) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.action_add_discourse))
                }
            },
        ) { padding ->
            if (state.isLoading) {
                ThreadLoadingState(modifier = Modifier.padding(padding).fillMaxSize())
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
                                title = stringResource(Res.string.s_ac8895327b),
                                subtitle = stringResource(Res.string.s_6fe30db879),
                                metric = run {
                                    val enabledCount = state.descriptors.count { it.enabled }
                                    stringResource(Res.string.s_96d7780f27, enabledCount, state.descriptors.size)
                                },
                            )
                        }
                        if (state.descriptors.isEmpty()) {
                            item(key = "empty-sources") {
                                ModernEmptyState(
                                    icon = Icons.Default.Hub,
                                    title = stringResource(Res.string.s_7bda6019d9),
                                    description = stringResource(Res.string.s_3507ddb87f),
                                    action = {
                                        SaniouButton(
                                            onClick = { viewModel.onEvent(Event.AddDiscourse) },
                                            text = stringResource(Res.string.action_add_discourse),
                                        )
                                    },
                                )
                            }
                        }
                        items(state.descriptors, key = SourceDescriptor::id) { descriptor ->
                            SourceDescriptorCard(
                                descriptor = descriptor,
                                enabled = !state.isSaving,
                                onToggle = { viewModel.onEvent(Event.SetEnabled(descriptor.id, it)) },
                                onEdit = { viewModel.onEvent(Event.Edit(descriptor)) },
                                onDelete = { viewModel.onEvent(Event.Remove(descriptor.id)) },
                                modifier = threadAnimateItem(),
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
    modifier: Modifier = Modifier,
) {
    ThreadCard(modifier.fillMaxWidth()) {
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
                            if (descriptor.isBuiltIn) stringResource(Res.string.s_09ceea7644) else stringResource(Res.string.label_activity),
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
                    Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.action_edit))
                }
                IconButton(onClick = onDelete, enabled = enabled) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
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
                if (descriptor == null) stringResource(Res.string.action_add_discourse) else stringResource(Res.string.s_ae96a77def),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(Res.string.s_bb7f40922f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                enabled = descriptor == null,
                label = { Text(stringResource(Res.string.s_b4e08a4b66)) },
                placeholder = { Text(stringResource(Res.string.s_5fee7d843d)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.label_display_name)) },
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
                label = { Text(stringResource(Res.string.s_6780ab7294)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                SaniouTextButton(onClick = onDismiss, text = stringResource(Res.string.subscription_cancel))
                SaniouButton(
                    onClick = { onSave(id, name, url, apiKey) },
                    enabled = !isSaving && id.isNotBlank() && name.isNotBlank() && url.isNotBlank(),
                    loading = isSaving,
                    text = stringResource(Res.string.action_save),
                )
            }
        }
    }
}
