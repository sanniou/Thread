package ai.saniou.thread.feature.operations

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouOutlinedButton
import ai.saniou.coreui.widgets.SaniouTextButton
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import thread.composeapp.generated.resources.label_unread
import thread.composeapp.generated.resources.action_reply
import thread.composeapp.generated.resources.label_article
import thread.composeapp.generated.resources.label_topic
import thread.composeapp.generated.resources.s_01ca4b8486
import thread.composeapp.generated.resources.s_06b65ae162
import thread.composeapp.generated.resources.s_2426432239
import thread.composeapp.generated.resources.s_284b34e15f
import thread.composeapp.generated.resources.s_35050c33c1
import thread.composeapp.generated.resources.action_refresh
import thread.composeapp.generated.resources.s_456ee3d61d
import thread.composeapp.generated.resources.s_4e173af0bc
import thread.composeapp.generated.resources.s_67a0fbb9dc
import thread.composeapp.generated.resources.action_close
import thread.composeapp.generated.resources.s_6cff6c6817
import thread.composeapp.generated.resources.s_6d1fac16bc
import thread.composeapp.generated.resources.s_6dfb2feb93
import thread.composeapp.generated.resources.s_705fcd3dfc
import thread.composeapp.generated.resources.s_8dc0d03081
import thread.composeapp.generated.resources.s_9365071b8f
import thread.composeapp.generated.resources.s_9f6a681e2b
import thread.composeapp.generated.resources.s_bdf1267805
import thread.composeapp.generated.resources.s_be19319228
import thread.composeapp.generated.resources.s_c63f79e636
import thread.composeapp.generated.resources.s_d4289269a4
import thread.composeapp.generated.resources.s_d439a278bc
import thread.composeapp.generated.resources.s_d47379f917
import thread.composeapp.generated.resources.s_d84129b8be
import thread.composeapp.generated.resources.s_e0c29eaeb3
import thread.composeapp.generated.resources.s_e4b9d92f14
import thread.composeapp.generated.resources.s_e93afc81dc
import thread.composeapp.generated.resources.s_f57207adee
import thread.composeapp.generated.resources.s_fe5e010aa4
import thread.composeapp.generated.resources.s_5faee8f507
import thread.composeapp.generated.resources.s_6c7dcbb73a
import thread.composeapp.generated.resources.label_all
import thread.composeapp.generated.resources.s_8a662fdebf
import thread.composeapp.generated.resources.s_979b6bb444
import thread.composeapp.generated.resources.s_b796f2d4ca
import thread.composeapp.generated.resources.s_bbbd563c35
import thread.composeapp.generated.resources.s_caf701318e
import thread.composeapp.generated.resources.s_ee8161e985

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
                        title = stringResource(Res.string.s_bdf1267805),
                        eyebrow = stringResource(Res.string.s_d4289269a4),
                        subtitle = stringResource(Res.string.s_d439a278bc),
                        actions = {
                            SaniouOutlinedButton(
                                onClick = { viewModel.onEvent(Event.ExportDiagnostic) },
                                enabled = !state.isExportingDiagnostic,
                                loading = state.isExportingDiagnostic,
                            ) {
                                Icon(Icons.Default.BugReport, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.s_be19319228))
                            }
                            SaniouOutlinedButton(onClick = { navigator.push(SourceManagerPage()) }) {
                                Icon(Icons.Default.Hub, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.s_67a0fbb9dc))
                            }
                        },
                    )
                }
                item {
                    ContextHero(
                        icon = Icons.Default.MonitorHeart,
                        title = if (state.snapshot.failedRefreshCount == 0) stringResource(Res.string.s_2426432239) else stringResource(Res.string.s_6cff6c6817),
                        subtitle = stringResource(Res.string.s_8dc0d03081),
                        metric = stringResource(Res.string.s_6d1fac16bc, state.snapshot.sources.size, state.snapshot.cachedItemCount),
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
                            Text(stringResource(Res.string.s_f57207adee), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.s_fe5e010aa4), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text(stringResource(Res.string.s_4e173af0bc), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    state.snapshot.storageDirectory,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SaniouTextButton(
                                onClick = {
                                    clipboard.copyText(state.snapshot.storageDirectory)
                                },
                                enabled = state.snapshot.storageDirectory.isNotBlank(),
                            ) {
                                Icon(Icons.Default.ContentCopy, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.s_e0c29eaeb3))
                            }
                        }
                        Text(
                            stringResource(Res.string.s_705fcd3dfc),
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
                paneTitle = stringResource(Res.string.s_e93afc81dc),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(stringResource(Res.string.s_35050c33c1), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(Res.string.s_06b65ae162),
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
                        SaniouTextButton(
                            onClick = { viewModel.onEvent(Event.DiagnosticDismissed) },
                            text = stringResource(Res.string.action_close),
                        )
                        SaniouButton(onClick = {
                            clipboard.copyText(payload)
                            viewModel.onEvent(Event.DiagnosticDismissed)
                        }) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.s_6dfb2feb93))
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
        MetricCard(stringResource(Res.string.s_c63f79e636), sourceCount.toString(), Icons.Default.Hub, Modifier.weight(1f))
        MetricCard(stringResource(Res.string.s_d47379f917), activeCount.toString(), Icons.Default.Sync, Modifier.weight(1f))
        MetricCard(stringResource(Res.string.s_284b34e15f), failedCount.toString(), Icons.Default.Warning, Modifier.weight(1f))
        MetricCard(stringResource(Res.string.s_e4b9d92f14), cachedCount.toString(), Icons.Default.Speed, Modifier.weight(1f))
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
    val sourceContentDescription = stringResource(
        Res.string.s_ee8161e985,
        source.name,
        presentation.label,
        source.primaryItemCount,
    )
    ThreadCard(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = sourceContentDescription
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
                    buildString {
                        append(presentation.label)
                        append(" · ")
                        append(source.primaryItemCount)
                        append(' ')
                        append(
                            if (source.kind == ContentSourceKind.FORUM) {
                                stringResource(Res.string.label_topic)
                            } else {
                                stringResource(Res.string.label_article)
                            },
                        )
                        append(" · ")
                        append(source.secondaryItemCount)
                        append(' ')
                        append(
                            if (source.kind == ContentSourceKind.FORUM) {
                                stringResource(Res.string.action_reply)
                            } else {
                                stringResource(Res.string.label_unread)
                            },
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isWorking) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (source.enabled) {
                SaniouButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.action_refresh))
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
                stringResource(Res.string.s_9365071b8f, Instant.fromEpochMilliseconds(it).toRelativeTimeString()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        source.lastSuccessfulRefreshAtEpochMillis?.let {
            Text(
                stringResource(Res.string.s_456ee3d61d, Instant.fromEpochMilliseconds(it).toRelativeTimeString()) +
                    if (source.consecutiveFailureCount > 0) stringResource(Res.string.s_9f6a681e2b, source.consecutiveFailureCount) else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (source.consecutiveFailureCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        source.rateLimitUntilEpochMillis?.takeIf { it > kotlin.time.Clock.System.now().toEpochMilliseconds() }?.let {
            Text(
                stringResource(Res.string.s_01ca4b8486, Instant.fromEpochMilliseconds(it).toRelativeTimeString()),
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
                    SaniouTextButton(onClick = onClearDiagnostic, text = stringResource(Res.string.s_d84129b8be))
                }
            }
        }
    }
}

private data class HealthPresentation(val label: String, val icon: ImageVector, val tone: HealthTone)
private enum class HealthTone { GOOD, NEUTRAL, WARNING, ERROR }

@Composable
private fun SourceOperationalState.presentation() = when (this) {
    SourceOperationalState.READY -> HealthPresentation(stringResource(Res.string.s_b796f2d4ca), Icons.Default.CheckCircle, HealthTone.GOOD)
    SourceOperationalState.DISABLED -> HealthPresentation(stringResource(Res.string.s_6c7dcbb73a), Icons.Default.PauseCircle, HealthTone.NEUTRAL)
    SourceOperationalState.REFRESHING -> HealthPresentation(stringResource(Res.string.s_d47379f917), Icons.Default.Sync, HealthTone.GOOD)
    SourceOperationalState.OFFLINE -> HealthPresentation(stringResource(Res.string.s_8a662fdebf), Icons.Default.CloudOff, HealthTone.WARNING)
    SourceOperationalState.AUTHENTICATION_REQUIRED -> HealthPresentation(stringResource(Res.string.s_caf701318e), Icons.Default.Lock, HealthTone.ERROR)
    SourceOperationalState.RATE_LIMITED -> HealthPresentation(stringResource(Res.string.s_979b6bb444), Icons.Default.Speed, HealthTone.WARNING)
    SourceOperationalState.DEGRADED -> HealthPresentation(stringResource(Res.string.s_bbbd563c35), Icons.Default.Warning, HealthTone.ERROR)
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

@Composable
private fun OperationsContract.Filter.label(): String = when (this) {
    OperationsContract.Filter.ALL -> stringResource(Res.string.label_all)
    OperationsContract.Filter.ATTENTION -> stringResource(Res.string.s_284b34e15f)
    OperationsContract.Filter.FORUM -> stringResource(Res.string.s_5faee8f507)
    OperationsContract.Filter.READER -> "Reader"
}
