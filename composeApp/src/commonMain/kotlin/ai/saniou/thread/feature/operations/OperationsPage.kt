package ai.saniou.thread.feature.operations

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.forum.workflow.source.SourceManagerPage
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.feature.operations.OperationsContract.Event
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

object OperationsPage : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<OperationsViewModel>() }
        val state by viewModel.state.collectAsState()
        val snackbar = remember { SnackbarHostState() }
        val clipboard = rememberThreadClipboard()

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.onEvent(Event.MessageShown)
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = Dimens.page_vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PageHeader(
                        title = "来源运维",
                        eyebrow = "OPERATIONS",
                        subtitle = "连接器健康、缓存覆盖与刷新诊断集中在一个可恢复工作区",
                        actions = {
                            OutlinedButton(
                                onClick = { viewModel.onEvent(Event.ExportDiagnostic) },
                                enabled = !state.isExportingDiagnostic,
                            ) {
                                if (state.isExportingDiagnostic) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.BugReport, null)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("脱敏诊断")
                            }
                            OutlinedButton(onClick = { navigator.push(SourceManagerPage()) }) {
                                Icon(Icons.Default.Hub, null)
                                Spacer(Modifier.width(8.dp))
                                Text("管理论坛来源")
                            }
                        },
                    )
                }
                item {
                    ContextHero(
                        icon = Icons.Default.MonitorHeart,
                        title = if (state.snapshot.failedRefreshCount == 0) "内容管线运行正常" else "部分来源需要关注",
                        subtitle = "失败按来源隔离；缓存内容、其他连接器与全局搜索不会被中断。",
                        metric = "${state.snapshot.sources.size} 来源 · ${state.snapshot.cachedItemCount} 缓存",
                    )
                }
                item {
                    OperationsMetrics(
                        sourceCount = state.snapshot.sources.size,
                        activeCount = state.snapshot.activeRefreshCount,
                        failedCount = state.snapshot.failedRefreshCount,
                        cachedCount = state.snapshot.cachedItemCount,
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OperationsContract.Filter.entries.forEach { filter ->
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
                if (state.visibleSources.isEmpty()) {
                    item {
                        ThreadCard(Modifier.fillMaxWidth()) {
                            Text("当前筛选没有来源", style = MaterialTheme.typography.titleMedium)
                            Text("所有已注册来源都处于稳定或停用状态。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.visibleSources, key = { "${it.kind}:${it.id}" }) { source ->
                        SourceHealthCard(
                            source = source,
                            isWorking = source.id in state.workingSourceIds,
                            onRetry = { viewModel.onEvent(Event.Retry(source)) },
                            onClearDiagnostic = { viewModel.onEvent(Event.ClearDiagnostic(source.id)) },
                        )
                    }
                }
                item {
                    ThreadCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("应用数据目录", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    state.snapshot.storageDirectory,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(
                                onClick = {
                                    clipboard.copyText(state.snapshot.storageDirectory)
                                },
                                enabled = state.snapshot.storageDirectory.isNotBlank(),
                            ) {
                                Icon(Icons.Default.ContentCopy, null)
                                Spacer(Modifier.width(6.dp))
                                Text("复制路径")
                            }
                        }
                        Text(
                            "数据库、缓存元数据与跨平台备份都使用明确的数据边界；Desktop 首次升级会迁移旧工作目录数据库。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        state.diagnosticPayload?.let { payload ->
            AdaptiveModal(
                onDismissRequest = { viewModel.onEvent(Event.DiagnosticDismissed) },
                paneTitle = "脱敏诊断预览",
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("可安全分享的运行快照", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "仅包含来源状态、缓存计数与刷新结果；账号、Cookie、令牌、内容正文和本地绝对路径不会导出。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        SelectionContainer {
                            Text(
                                payload,
                                Modifier.padding(16.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.onEvent(Event.DiagnosticDismissed) }) { Text("关闭") }
                        Button(onClick = {
                            clipboard.copyText(payload)
                            viewModel.onEvent(Event.DiagnosticDismissed)
                        }) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(6.dp))
                            Text("复制诊断")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationsMetrics(sourceCount: Int, activeCount: Int, failedCount: Int, cachedCount: Long) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard("来源", sourceCount.toString(), Icons.Default.Hub, Modifier.weight(1f))
        MetricCard("刷新中", activeCount.toString(), Icons.Default.Sync, Modifier.weight(1f))
        MetricCard("需关注", failedCount.toString(), Icons.Default.Warning, Modifier.weight(1f))
        MetricCard("缓存条目", cachedCount.toString(), Icons.Default.Speed, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier.widthIn(min = 150.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SourceHealthCard(
    source: SourceHealth,
    isWorking: Boolean,
    onRetry: () -> Unit,
    onClearDiagnostic: () -> Unit,
) {
    val presentation = source.state.presentation()
    ThreadCard(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${source.name}，${presentation.label}，${source.primaryItemCount} 条主要缓存"
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = presentation.containerColor()) {
                Icon(
                    presentation.icon,
                    null,
                    Modifier.padding(10.dp).size(22.dp),
                    tint = presentation.contentColor(),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (source.kind == ContentSourceKind.FORUM) "FORUM" else "READER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "${presentation.label} · ${source.primaryItemCount} ${if (source.kind == ContentSourceKind.FORUM) "主题" else "文章"} · ${source.secondaryItemCount} ${if (source.kind == ContentSourceKind.FORUM) "回复" else "未读"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isWorking) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (source.enabled) {
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("刷新")
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            source.capabilities.forEach { capability ->
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text(capability, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        source.lastContentAtEpochMillis?.let {
            Text(
                "最近内容 ${Instant.fromEpochMilliseconds(it).toRelativeTimeString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        source.lastSuccessfulRefreshAtEpochMillis?.let {
            Text(
                "上次成功 ${Instant.fromEpochMilliseconds(it).toRelativeTimeString()}" +
                    if (source.consecutiveFailureCount > 0) " · 连续失败 ${source.consecutiveFailureCount} 次" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (source.consecutiveFailureCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        source.rateLimitUntilEpochMillis?.takeIf { it > kotlin.time.Clock.System.now().toEpochMilliseconds() }?.let {
            Text(
                "限流恢复 ${Instant.fromEpochMilliseconds(it).toRelativeTimeString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        source.message?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = onClearDiagnostic) { Text("忽略") }
                }
            }
        }
    }
}

private data class HealthPresentation(val label: String, val icon: ImageVector, val tone: HealthTone)
private enum class HealthTone { GOOD, NEUTRAL, WARNING, ERROR }

private fun SourceOperationalState.presentation() = when (this) {
    SourceOperationalState.READY -> HealthPresentation("就绪", Icons.Default.CheckCircle, HealthTone.GOOD)
    SourceOperationalState.DISABLED -> HealthPresentation("已停用", Icons.Default.PauseCircle, HealthTone.NEUTRAL)
    SourceOperationalState.REFRESHING -> HealthPresentation("刷新中", Icons.Default.Sync, HealthTone.GOOD)
    SourceOperationalState.OFFLINE -> HealthPresentation("网络离线", Icons.Default.CloudOff, HealthTone.WARNING)
    SourceOperationalState.AUTHENTICATION_REQUIRED -> HealthPresentation("需要登录", Icons.Default.Lock, HealthTone.ERROR)
    SourceOperationalState.RATE_LIMITED -> HealthPresentation("请求受限", Icons.Default.Speed, HealthTone.WARNING)
    SourceOperationalState.DEGRADED -> HealthPresentation("服务异常", Icons.Default.Warning, HealthTone.ERROR)
}

@Composable
private fun HealthPresentation.containerColor(): Color = when (tone) {
    HealthTone.GOOD -> MaterialTheme.colorScheme.secondaryContainer
    HealthTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
    HealthTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    HealthTone.ERROR -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun HealthPresentation.contentColor(): Color = when (tone) {
    HealthTone.GOOD -> MaterialTheme.colorScheme.onSecondaryContainer
    HealthTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    HealthTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
    HealthTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
}

private fun OperationsContract.Filter.label(): String = when (this) {
    OperationsContract.Filter.ALL -> "全部"
    OperationsContract.Filter.ATTENTION -> "需关注"
    OperationsContract.Filter.FORUM -> "论坛"
    OperationsContract.Filter.READER -> "Reader"
}
