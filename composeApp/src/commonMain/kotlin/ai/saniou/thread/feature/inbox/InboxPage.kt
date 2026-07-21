package ai.saniou.thread.feature.inbox

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.LocalThreadUiPreferences
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.FeedTopicRoute
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.LinkResolution
import ai.saniou.thread.domain.model.inbox.InboxEvent
import ai.saniou.thread.domain.model.inbox.InboxKind
import ai.saniou.thread.domain.repository.ContentLinkRepository
import ai.saniou.thread.feature.inbox.InboxContract.Event
import ai.saniou.thread.feature.social.SocialDetailPage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance

object InboxPage : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<InboxViewModel>() }
        val linkRepository = remember(di) { di.direct.instance<ContentLinkRepository>() }
        val state by viewModel.state.collectAsState()
        val inbox = viewModel.events.collectAsLazyPagingItems()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current
        val ui = LocalThreadUiPreferences.current

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.onEvent(Event.MessageShown)
            }
        }

        fun open(reference: ContentReference?) {
            if (reference == null) return
            scope.launch {
                when (val resolution = linkRepository.resolveReference(reference)) {
                    is LinkResolution.External -> uriHandler.openUri(resolution.url)
                    is LinkResolution.Unsupported -> snackbar.showSnackbar(resolution.reason)
                    is LinkResolution.Internal -> when (resolution.reference.kind) {
                        ContentReferenceKind.TOPIC -> navigator.push(
                            FeedTopicRoute(checkNotNull(resolution.reference.sourceId), resolution.reference.id)
                        )
                        ContentReferenceKind.COMMENT -> navigator.push(
                            FeedTopicRoute(
                                checkNotNull(resolution.reference.sourceId),
                                checkNotNull(resolution.reference.parentId),
                            )
                        )
                        ContentReferenceKind.ARTICLE -> navigator.push(ArticleDetailPage(resolution.reference.id))
                        ContentReferenceKind.SOCIAL_POST -> navigator.push(
                            SocialDetailPage(
                                sourceId = checkNotNull(resolution.reference.sourceId),
                                postId = resolution.reference.id,
                            ),
                        )
                        ContentReferenceKind.EXTERNAL_URL -> Unit
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(ui.sectionSpacing),
            ) {
                item {
                    PageHeader(
                        eyebrow = "收件箱",
                        title = "通知收件箱",
                        subtitle = "公告、回复、订阅和 Reader 更新共用一条可筛选、可静音、离线优先的时间线。",
                        actions = {
                            SaniouTextButton(onClick = { viewModel.onEvent(Event.MarkAllRead) }) {
                                Icon(Icons.Default.DoneAll, null)
                                Spacer(Modifier.width(6.dp))
                                Text("全部已读")
                            }
                        },
                    )
                }
                item {
                    ContextHero(
                        title = if (state.summary.unread == 0) "收件箱已清空" else "${state.summary.unread} 条未读更新",
                        subtitle = "共 ${state.summary.total} 条，${state.summary.muted} 条来自静音来源；所有计数由 SQLDelight 实时驱动。",
                        icon = if (state.summary.unread == 0) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    )
                }
                item {
                    InboxMetrics(state.summary.total, state.summary.unread, state.summary.sourceCounts.size, state.summary.muted)
                }
                item {
                    OutlinedTextField(
                        value = state.filter.query,
                        onValueChange = { viewModel.onEvent(Event.QueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text("搜索通知标题或摘要…") },
                        singleLine = true,
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.filter.unreadOnly,
                            onClick = { viewModel.onEvent(Event.UnreadOnlyChanged(!state.filter.unreadOnly)) },
                            label = { Text("仅未读") },
                        )
                        FilterChip(
                            selected = state.filter.includeMuted,
                            onClick = { viewModel.onEvent(Event.IncludeMutedChanged(!state.filter.includeMuted)) },
                            label = { Text("显示静音") },
                        )
                        InboxKind.entries.forEach { kind ->
                            FilterChip(
                                selected = state.filter.kind == kind,
                                onClick = { viewModel.onEvent(Event.KindChanged(kind.takeUnless { state.filter.kind == kind })) },
                                label = { Text(kind.label()) },
                            )
                        }
                    }
                }
                if (state.summary.sourceCounts.isNotEmpty()) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { viewModel.onEvent(Event.SourceChanged(null)) },
                                label = { Text("全部来源") },
                            )
                            state.summary.sourceCounts.forEach { source ->
                                AssistChip(
                                    onClick = { viewModel.onEvent(Event.SourceChanged(source.sourceId.takeUnless { state.filter.sourceId == it })) },
                                    label = { Text("${source.sourceId} · ${source.unread}") },
                                    leadingIcon = if (source.muted) ({ Icon(Icons.AutoMirrored.Filled.VolumeOff, null, Modifier.size(18.dp)) }) else null,
                                )
                            }
                        }
                    }
                }
                when (val refresh = inbox.loadState.refresh) {
                    is LoadState.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    is LoadState.Error -> item {
                        ThreadCard(Modifier.fillMaxWidth()) {
                            Text("无法读取收件箱", style = MaterialTheme.typography.titleMedium)
                            Text(refresh.error.message ?: "数据库分页失败", color = MaterialTheme.colorScheme.error)
                            SaniouButton(onClick = inbox::retry, text = "重试")
                        }
                    }
                    else -> if (inbox.itemCount == 0) item {
                        ThreadCard(Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.MarkEmailRead, null, tint = MaterialTheme.colorScheme.primary)
                            Text("当前筛选下没有通知", style = MaterialTheme.typography.titleMedium)
                            Text("刷新 Reader 或收到社区更新后，新事件会自动汇入这里。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(
                    count = inbox.itemCount,
                    key = { index -> inbox.peek(index)?.id ?: "inbox-$index" },
                ) { index ->
                    inbox[index]?.let { event ->
                        InboxEventCard(
                            event = event,
                            onOpen = {
                                viewModel.onEvent(Event.MarkRead(event.id))
                                open(event.reference)
                            },
                            onToggleRead = { viewModel.onEvent(Event.MarkRead(event.id, !event.isRead)) },
                            onToggleMute = { viewModel.onEvent(Event.SetSourceMuted(event.sourceId, !event.muted)) },
                            onDelete = { viewModel.onEvent(Event.Delete(event.id)) },
                        )
                    }
                }
                if (inbox.loadState.append is LoadState.Loading) item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun InboxMetrics(total: Int, unread: Int, sources: Int, muted: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("全部" to total, "未读" to unread, "来源" to sources, "静音" to muted).forEach { (label, value) ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InboxEventCard(
    event: InboxEvent,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onToggleMute: () -> Unit,
    onDelete: () -> Unit,
) {
    ThreadCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = event.reference != null, onClick = onOpen),
        containerColor = if (event.isRead) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(event.kind.icon(), null, Modifier.size(22.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (event.isRead) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (event.reference != null) Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                }
                if (event.summary.isNotBlank()) Text(
                    event.summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${event.sourceId} · ${event.kind.label()} · ${event.occurredAt.toRelativeTimeString()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = onToggleRead) {
                    Icon(if (event.isRead) Icons.Default.Notifications else Icons.Default.MarkEmailRead, if (event.isRead) "标为未读" else "标为已读")
                }
                IconButton(onClick = onToggleMute) {
                    Icon(if (event.muted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, if (event.muted) "取消静音来源" else "静音来源")
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "删除通知") }
            }
        }
    }
}

private fun InboxKind.label(): String = when (this) {
    InboxKind.ANNOUNCEMENT -> "公告"
    InboxKind.MENTION -> "提及"
    InboxKind.REPLY -> "回复"
    InboxKind.SUBSCRIPTION_UPDATE -> "订阅"
    InboxKind.READER_UPDATE -> "Reader"
    InboxKind.SYSTEM -> "系统"
}

private fun InboxKind.icon(): ImageVector = when (this) {
    InboxKind.ANNOUNCEMENT -> Icons.Default.Campaign
    InboxKind.MENTION -> Icons.Default.AlternateEmail
    InboxKind.REPLY -> Icons.AutoMirrored.Filled.Reply
    InboxKind.SUBSCRIPTION_UPDATE -> Icons.Default.Notifications
    InboxKind.READER_UPDATE -> Icons.Default.RssFeed
    InboxKind.SYSTEM -> Icons.Default.Info
}
