package ai.saniou.thread.feature.activity

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.ThreadCard
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                        eyebrow = "ACTIVITY",
                        title = "活动中心",
                        subtitle = "刷新、身份、草稿与数据迁移共享一条可恢复的跨平台任务流。",
                        actions = {
                            TextButton(onClick = { viewModel.onEvent(Event.ClearCompleted) }) {
                                Icon(Icons.Default.DeleteOutline, null)
                                Spacer(Modifier.width(6.dp))
                                Text("清理已完成")
                            }
                        },
                    )
                }
                item {
                    ContextHero(
                        title = if (state.snapshot.actionRequiredCount + state.snapshot.failedCount == 0) {
                            "所有工作流均可继续"
                        } else {
                            "${state.snapshot.actionRequiredCount + state.snapshot.failedCount} 项需要处理"
                        },
                        subtitle = "后台动作采用冲突域串行化；进程中断的任务会显式转为可重试状态。",
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
                        placeholder = { Text("筛选来源、任务或草稿…") },
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
                            Text("当前视图没有活动", style = MaterialTheme.typography.titleMedium)
                            Text("新的刷新、认证、草稿和数据任务会自动出现在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.visibleItems, key = ActivityItem::id) { item ->
                        ActivityCard(
                            item = item,
                            working = item.primaryAction?.conflictKey in state.runningConflictKeys,
                            onOpen = { open(item) },
                            onExecute = { viewModel.onEvent(Event.Execute(it)) },
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
                title = { Text(if (request.danger == ProductActionDanger.DESTRUCTIVE) "确认永久操作" else "确认数据变更") },
                text = { Text(request.confirmationText()) },
                confirmButton = {
                    Button(onClick = { viewModel.onEvent(Event.ConfirmDangerAction) }) { Text("继续") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(Event.DismissDangerAction) }) { Text("取消") }
                },
            )
        }

        state.outputPayload?.let { payload ->
            AdaptiveModal(
                onDismissRequest = { viewModel.onEvent(Event.DismissOutput) },
                paneTitle = state.outputTitle ?: "动作输出",
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("可复制输出", style = MaterialTheme.typography.headlineSmall)
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
                        TextButton(onClick = { viewModel.onEvent(Event.DismissOutput) }) { Text("关闭") }
                        Button(onClick = {
                            clipboard.copyText(payload)
                            viewModel.onEvent(Event.DismissOutput)
                        }) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(6.dp))
                            Text("复制")
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
        ActivityMetric("执行中", running, Icons.Default.Sync, MaterialTheme.colorScheme.primary)
        ActivityMetric("需处理", attention, Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error)
        ActivityMetric("草稿", drafts, Icons.Default.EditNote, MaterialTheme.colorScheme.tertiary)
        ActivityMetric("已完成", completed, Icons.Default.History, MaterialTheme.colorScheme.secondary)
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
                Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
) {
    val presentation = item.presentation()
    ThreadCard(
        modifier = Modifier.fillMaxWidth().semantics {
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
                TextButton(onClick = { onExecute(action) }, enabled = action.conflictKey != item.primaryAction?.conflictKey || !working) {
                    Text(if (action.type == ProductActionType.DISCARD_DRAFT) "丢弃" else "忽略")
                }
            }
            if (item.deepLink != null) {
                OutlinedButton(onClick = onOpen) {
                    Icon(if (item.kind == ActivityKind.DRAFT) Icons.Default.EditNote else Icons.AutoMirrored.Filled.OpenInNew, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (item.kind == ActivityKind.DRAFT) "继续" else "打开")
                }
            }
            item.primaryAction?.let { action ->
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onExecute(action) }, enabled = !working) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (working) "执行中" else "执行")
                }
            }
        }
    }
}

@Composable
private fun IdentityStrip(identities: List<ai.saniou.thread.domain.model.activity.SourceIdentityStatus>) {
    if (identities.none { it.supportsLogin }) return
    ThreadCard(Modifier.fillMaxWidth()) {
        Text("来源身份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("身份状态不再从异常文案临时推断；刷新与登录只更新这一份显式事实。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun ActivityItem.presentation(): ActivityPresentation = when (state) {
    ActivityState.RUNNING -> ActivityPresentation("执行中", Icons.Default.Sync, ActivityTone.INFO)
    ActivityState.ACTION_REQUIRED -> ActivityPresentation("需要操作", Icons.Default.Lock, ActivityTone.ERROR)
    ActivityState.FAILED -> ActivityPresentation("可重试", Icons.Default.CloudOff, ActivityTone.WARNING)
    ActivityState.READY -> ActivityPresentation("可继续", if (kind == ActivityKind.DRAFT) Icons.Default.EditNote else Icons.Default.NotificationsActive, ActivityTone.INFO)
    ActivityState.COMPLETED -> ActivityPresentation("已完成", Icons.Default.CheckCircle, ActivityTone.GOOD)
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

private fun ActivityCenterContract.Filter.label(): String = when (this) {
    ActivityCenterContract.Filter.ATTENTION -> "待处理"
    ActivityCenterContract.Filter.ALL -> "全部"
    ActivityCenterContract.Filter.RUNNING -> "执行中"
    ActivityCenterContract.Filter.DRAFTS -> "草稿"
    ActivityCenterContract.Filter.HISTORY -> "已完成"
}

private fun IdentityValidity.label(): String = when (this) {
    IdentityValidity.NOT_APPLICABLE -> "无需登录"
    IdentityValidity.ANONYMOUS -> "匿名"
    IdentityValidity.VALID -> "有效"
    IdentityValidity.EXPIRED -> "已过期"
    IdentityValidity.DISABLED -> "已停用"
}

private fun ProductActionRequest.confirmationText(): String = when (type) {
    ProductActionType.DISCARD_DRAFT -> "此草稿只保存在当前设备。丢弃后无法恢复。"
    ProductActionType.RESTORE_FROM_WEBDAV -> "远端数据会与本地来源、订阅、收藏和阅读状态合并。"
    ProductActionType.IMPORT_USER_DATA -> "导入快照会更新本地来源和用户数据。"
    ProductActionType.IMPORT_READER_SUBSCRIPTIONS -> "导入会新增或更新同 URL 的 Reader 订阅。"
    ProductActionType.SET_SOURCE_ENABLED -> "停用来源后，其导航入口和聚合内容将暂时隐藏。"
    else -> "此动作会修改本地数据，是否继续？"
}
