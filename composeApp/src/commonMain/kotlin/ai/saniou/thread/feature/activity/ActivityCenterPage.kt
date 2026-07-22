package ai.saniou.thread.feature.activity

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouDangerButton
import ai.saniou.coreui.widgets.SaniouOutlinedButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.forum.workflow.post.PostPage
import ai.saniou.thread.ForumUserRoute
import ai.saniou.thread.domain.model.activity.ActivityItem
import ai.saniou.thread.domain.model.activity.ActivityKind
import ai.saniou.thread.domain.model.activity.ActivityState
import ai.saniou.thread.domain.model.activity.IdentityValidity
import ai.saniou.thread.domain.model.activity.ProductActionDanger
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.feature.activity.ActivityCenterContract.Event
import ai.saniou.thread.feature.operations.OperationsPage
import ai.saniou.thread.feature.settings.SyncSettingsPage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_0b46d8806e
import thread.composeapp.generated.resources.label_draft
import thread.composeapp.generated.resources.label_activity_center
import thread.composeapp.generated.resources.s_149b3c638f
import thread.composeapp.generated.resources.label_working
import thread.composeapp.generated.resources.action_continue
import thread.composeapp.generated.resources.s_28febba225
import thread.composeapp.generated.resources.s_35405c37e2
import thread.composeapp.generated.resources.s_39f95e3775
import thread.composeapp.generated.resources.s_3f7b376d3e
import thread.composeapp.generated.resources.s_45ad45848d
import thread.composeapp.generated.resources.action_cancel
import thread.composeapp.generated.resources.action_copy
import thread.composeapp.generated.resources.s_62e27e2865
import thread.composeapp.generated.resources.s_65fc81e161
import thread.composeapp.generated.resources.action_close
import thread.composeapp.generated.resources.confirm_permanent
import thread.composeapp.generated.resources.s_8a6e4f192f
import thread.composeapp.generated.resources.s_ab0a804137
import thread.composeapp.generated.resources.confirm_data_change
import thread.composeapp.generated.resources.s_afa43bc96e
import thread.composeapp.generated.resources.label_activity_short
import thread.composeapp.generated.resources.s_c840cbecba
import thread.composeapp.generated.resources.s_d2ade1f772
import thread.composeapp.generated.resources.action_dismiss
import thread.composeapp.generated.resources.label_completed
import thread.composeapp.generated.resources.s_ea4899468c
import thread.composeapp.generated.resources.s_ed5909bac1
import thread.composeapp.generated.resources.s_1354374f76
import thread.composeapp.generated.resources.s_39e846f9a0
import thread.composeapp.generated.resources.s_4722686b0c
import thread.composeapp.generated.resources.s_59a9eb4e65
import thread.composeapp.generated.resources.s_5ffe0b99be
import thread.composeapp.generated.resources.s_62c7d33506
import thread.composeapp.generated.resources.s_656c40a504
import thread.composeapp.generated.resources.label_disabled
import thread.composeapp.generated.resources.label_all
import thread.composeapp.generated.resources.s_89a2b24d0c
import thread.composeapp.generated.resources.s_8ecb358ed8
import thread.composeapp.generated.resources.s_9117f23d72
import thread.composeapp.generated.resources.s_ad385d382a
import thread.composeapp.generated.resources.s_bbdc7b495b
import thread.composeapp.generated.resources.s_d04bd6349b
import thread.composeapp.generated.resources.s_ed15fd8c6c

object ActivityCenterPage : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<ActivityCenterViewModel>() }
        val state by viewModel.state.collectAsState()
        val snackbar = remember { SnackbarHostState() }
        val clipboard = rememberThreadClipboard()

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.onEvent(Event.MessageShown)
            }
        }

        fun open(item: ActivityItem) {
            val deepLink = item.deepLink ?: return
            val draftKey = deepLink.draftKey
            when {
                draftKey != null -> navigator.push(
                    PostPage(
                        sourceId = draftKey.sourceId,
                        channelId = draftKey.targetId.takeIf { draftKey.targetKind == PostDraftTargetKind.CHANNEL },
                        topicId = draftKey.targetId.takeIf { draftKey.targetKind == PostDraftTargetKind.TOPIC },
                    )
                )
                item.kind == ActivityKind.AUTHENTICATION ->
                    deepLink.sourceId?.let { navigator.push(ForumUserRoute(it)) }
                deepLink.destination == WorkspaceDestination.OPERATIONS -> navigator.push(OperationsPage)
                deepLink.destination == WorkspaceDestination.SETTINGS -> navigator.push(SyncSettingsPage())
            }
        }

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PageHeader(
                        eyebrow = stringResource(Res.string.label_activity_short),
                        title = stringResource(Res.string.label_activity_center),
                        subtitle = stringResource(Res.string.s_39f95e3775),
                        actions = {
                            SaniouTextButton(onClick = { viewModel.onEvent(Event.ClearCompleted) }) {
                                Icon(Icons.Default.DeleteOutline, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.s_149b3c638f))
                            }
                        },
                    )
                }
                item {
                    ContextHero(
                        title = if (state.snapshot.actionRequiredCount + state.snapshot.failedCount == 0) {
                            stringResource(Res.string.s_0b46d8806e)
                        } else {
                            stringResource(Res.string.s_ab0a804137, state.snapshot.actionRequiredCount + state.snapshot.failedCount)
                        },
                        subtitle = stringResource(Res.string.s_c840cbecba),
                        icon = Icons.Default.NotificationsActive,
                    )
                }
                item {
                    ActivityMetrics(
                        running = state.snapshot.runningCount,
                        attention = state.snapshot.actionRequiredCount + state.snapshot.failedCount,
                        drafts = state.snapshot.draftCount,
                        completed = state.snapshot.items.count { it.state == ActivityState.COMPLETED },
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onEvent(Event.QueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text(stringResource(Res.string.s_8a6e4f192f)) },
                        singleLine = true,
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityCenterContract.Filter.entries.forEach { filter ->
                            AssistChip(
                                onClick = { viewModel.onEvent(Event.FilterChanged(filter)) },
                                label = { Text(filter.label()) },
                                leadingIcon = if (state.filter == filter) {
                                    { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)) }
                                } else null,
                            )
                        }
                    }
                }
                if (state.visibleItems.isEmpty()) {
                    item {
                        ThreadCard(Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(Res.string.s_62e27e2865), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.s_35405c37e2), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.visibleItems, key = ActivityItem::id) { item ->
                        ActivityCard(
                            item = item,
                            working = item.primaryAction?.conflictKey in state.runningConflictKeys,
                            onOpen = { open(item) },
                            onExecute = { viewModel.onEvent(Event.Execute(it)) },
                            modifier = threadAnimateItem(),
                        )
                    }
                }
                item { IdentityStrip(state.snapshot.identities) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }

        state.pendingDangerAction?.let { request ->
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(Event.DismissDangerAction) },
                icon = { Icon(Icons.Default.Warning, null) },
                title = { Text(if (request.danger == ProductActionDanger.DESTRUCTIVE) stringResource(Res.string.confirm_permanent) else stringResource(Res.string.confirm_data_change)) },
                text = { Text(request.confirmationText()) },
                confirmButton = {
                    if (request.danger == ProductActionDanger.DESTRUCTIVE) {
                        SaniouDangerButton(
                            onClick = { viewModel.onEvent(Event.ConfirmDangerAction) },
                            text = stringResource(Res.string.action_continue),
                        )
                    } else {
                        SaniouButton(
                            onClick = { viewModel.onEvent(Event.ConfirmDangerAction) },
                            text = stringResource(Res.string.action_continue),
                        )
                    }
                },
                dismissButton = {
                    SaniouTextButton(
                        onClick = { viewModel.onEvent(Event.DismissDangerAction) },
                        text = stringResource(Res.string.action_cancel),
                    )
                },
            )
        }

        state.outputPayload?.let { payload ->
            AdaptiveModal(
                onDismissRequest = { viewModel.onEvent(Event.DismissOutput) },
                paneTitle = state.outputTitle ?: stringResource(Res.string.s_d2ade1f772),
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(Res.string.s_afa43bc96e), style = MaterialTheme.typography.headlineSmall)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                payload,
                                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        SaniouTextButton(
                            onClick = { viewModel.onEvent(Event.DismissOutput) },
                            text = stringResource(Res.string.action_close),
                        )
                        SaniouButton(onClick = {
                            clipboard.copyText(payload)
                            viewModel.onEvent(Event.DismissOutput)
                        }) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.action_copy))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMetrics(running: Int, attention: Int, drafts: Int, completed: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ActivityMetric(stringResource(Res.string.label_working), running, Icons.Default.Sync, MaterialTheme.colorScheme.primary)
        ActivityMetric(stringResource(Res.string.s_ed5909bac1), attention, Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error)
        ActivityMetric(stringResource(Res.string.label_draft), drafts, Icons.Default.EditNote, MaterialTheme.colorScheme.tertiary)
        ActivityMetric(stringResource(Res.string.label_completed), completed, Icons.Default.History, MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun ActivityMetric(label: String, count: Int, icon: ImageVector, tint: Color) {
    Surface(
        modifier = Modifier.widthIn(min = 150.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = tint)
            Column {
                Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActivityCard(
    item: ActivityItem,
    working: Boolean,
    onOpen: () -> Unit,
    onExecute: (ProductActionRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = item.presentation()
    ThreadCard(
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = "${item.title}，${presentation.label}，${item.summary}"
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = presentation.containerColor()) {
                Icon(presentation.icon, null, Modifier.padding(11.dp).size(22.dp), tint = presentation.contentColor())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(presentation.label, style = MaterialTheme.typography.labelSmall, color = presentation.contentColor())
                }
                Text(
                    item.summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    Instant.fromEpochMilliseconds(item.occurredAtEpochMillis).toRelativeTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (working || item.state == ActivityState.RUNNING) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            item.secondaryAction?.let { action ->
                SaniouTextButton(
                    onClick = { onExecute(action) },
                    enabled = action.conflictKey != item.primaryAction?.conflictKey || !working,
                    text = if (action.type == ProductActionType.DISCARD_DRAFT) stringResource(Res.string.s_ea4899468c) else stringResource(Res.string.action_dismiss),
                )
            }
            if (item.deepLink != null) {
                SaniouOutlinedButton(onClick = onOpen) {
                    Icon(if (item.kind == ActivityKind.DRAFT) Icons.Default.EditNote else Icons.AutoMirrored.Filled.OpenInNew, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (item.kind == ActivityKind.DRAFT) stringResource(Res.string.action_continue) else stringResource(Res.string.s_65fc81e161))
                }
            }
            item.primaryAction?.let { action ->
                Spacer(Modifier.width(8.dp))
                SaniouButton(
                    onClick = { onExecute(action) },
                    enabled = !working,
                    loading = working,
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.s_28febba225))
                }
            }
        }
    }
}

@Composable
private fun IdentityStrip(identities: List<ai.saniou.thread.domain.model.activity.SourceIdentityStatus>) {
    if (identities.none { it.supportsLogin }) return
    ThreadCard(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.s_45ad45848d), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.s_3f7b376d3e), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            identities.filter { it.supportsLogin }.forEach { identity ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (identity.validity == IdentityValidity.VALID) Icons.Default.CheckCircle else Icons.Default.Lock,
                            null,
                            Modifier.size(18.dp),
                            tint = if (identity.validity == IdentityValidity.VALID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(7.dp))
                        Text("${identity.sourceName} · ${identity.validity.label()}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private data class ActivityPresentation(val label: String, val icon: ImageVector, val tone: ActivityTone)
private enum class ActivityTone { GOOD, INFO, WARNING, ERROR }

@Composable
private fun ActivityItem.presentation(): ActivityPresentation = when (state) {
    ActivityState.RUNNING -> ActivityPresentation(stringResource(Res.string.label_working), Icons.Default.Sync, ActivityTone.INFO)
    ActivityState.ACTION_REQUIRED -> ActivityPresentation(stringResource(Res.string.s_8ecb358ed8), Icons.Default.Lock, ActivityTone.ERROR)
    ActivityState.FAILED -> ActivityPresentation(stringResource(Res.string.s_5ffe0b99be), Icons.Default.CloudOff, ActivityTone.WARNING)
    ActivityState.READY -> ActivityPresentation(stringResource(Res.string.s_ed15fd8c6c), if (kind == ActivityKind.DRAFT) Icons.Default.EditNote else Icons.Default.NotificationsActive, ActivityTone.INFO)
    ActivityState.COMPLETED -> ActivityPresentation(stringResource(Res.string.label_completed), Icons.Default.CheckCircle, ActivityTone.GOOD)
}

@Composable
private fun ActivityPresentation.containerColor(): Color = when (tone) {
    ActivityTone.GOOD -> MaterialTheme.colorScheme.secondaryContainer
    ActivityTone.INFO -> MaterialTheme.colorScheme.primaryContainer
    ActivityTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    ActivityTone.ERROR -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun ActivityPresentation.contentColor(): Color = when (tone) {
    ActivityTone.GOOD -> MaterialTheme.colorScheme.onSecondaryContainer
    ActivityTone.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
    ActivityTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
    ActivityTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
private fun ActivityCenterContract.Filter.label(): String = when (this) {
    ActivityCenterContract.Filter.ATTENTION -> stringResource(Res.string.s_59a9eb4e65)
    ActivityCenterContract.Filter.ALL -> stringResource(Res.string.label_all)
    ActivityCenterContract.Filter.RUNNING -> stringResource(Res.string.label_working)
    ActivityCenterContract.Filter.DRAFTS -> stringResource(Res.string.label_draft)
    ActivityCenterContract.Filter.HISTORY -> stringResource(Res.string.label_completed)
}

@Composable
private fun IdentityValidity.label(): String = when (this) {
    IdentityValidity.NOT_APPLICABLE -> stringResource(Res.string.s_89a2b24d0c)
    IdentityValidity.ANONYMOUS -> stringResource(Res.string.s_9117f23d72)
    IdentityValidity.VALID -> stringResource(Res.string.s_ad385d382a)
    IdentityValidity.EXPIRED -> stringResource(Res.string.s_1354374f76)
    IdentityValidity.DISABLED -> stringResource(Res.string.label_disabled)
}

@Composable
private fun ProductActionRequest.confirmationText(): String = when (type) {
    ProductActionType.DISCARD_DRAFT -> stringResource(Res.string.s_bbdc7b495b)
    ProductActionType.RESTORE_FROM_WEBDAV -> stringResource(Res.string.s_4722686b0c)
    ProductActionType.IMPORT_USER_DATA -> stringResource(Res.string.s_d04bd6349b)
    ProductActionType.IMPORT_READER_SUBSCRIPTIONS -> stringResource(Res.string.s_62c7d33506)
    ProductActionType.SET_SOURCE_ENABLED -> stringResource(Res.string.s_39e846f9a0)
    else -> stringResource(Res.string.s_656c40a504)
}
