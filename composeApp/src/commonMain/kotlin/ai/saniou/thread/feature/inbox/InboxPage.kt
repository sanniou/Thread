package ai.saniou.thread.feature.inbox

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.LocalThreadUiPreferences
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadLoadingState
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.coreui.state.PagingAppendState
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
import org.jetbrains.compose.resources.stringResource
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_1a0ea4638c
import thread.composeapp.generated.resources.label_unread
import thread.composeapp.generated.resources.s_26fdb68077
import thread.composeapp.generated.resources.filter_unread_only
import thread.composeapp.generated.resources.s_3c0aaf62db
import thread.composeapp.generated.resources.s_41ca4f4fa4
import thread.composeapp.generated.resources.s_5274a19b11
import thread.composeapp.generated.resources.s_557ee228de
import thread.composeapp.generated.resources.s_64d2bc34d9
import thread.composeapp.generated.resources.label_all
import thread.composeapp.generated.resources.label_notification_inbox
import thread.composeapp.generated.resources.s_862ebc4065
import thread.composeapp.generated.resources.s_ac210d6e7d
import thread.composeapp.generated.resources.s_afdbd1ddec
import thread.composeapp.generated.resources.s_b1971f6896
import thread.composeapp.generated.resources.s_c4856b8590
import thread.composeapp.generated.resources.label_source
import thread.composeapp.generated.resources.s_ccb91c07d7
import thread.composeapp.generated.resources.label_inbox
import thread.composeapp.generated.resources.s_d01a73e67b
import thread.composeapp.generated.resources.s_d37c67c75a
import thread.composeapp.generated.resources.action_retry
import thread.composeapp.generated.resources.s_f5d9c71552
import thread.composeapp.generated.resources.s_f86d4f875a
import thread.composeapp.generated.resources.s_f8d22fd22e
import thread.composeapp.generated.resources.s_1a1f6dff78
import thread.composeapp.generated.resources.label_announcement
import thread.composeapp.generated.resources.s_5319af762d
import thread.composeapp.generated.resources.s_9f7ab435be
import thread.composeapp.generated.resources.action_reply

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
                        eyebrow = stringResource(Res.string.label_inbox),
                        title = stringResource(Res.string.label_notification_inbox),
                        subtitle = stringResource(Res.string.s_862ebc4065),
                        actions = {
                            SaniouTextButton(onClick = { viewModel.onEvent(Event.MarkAllRead) }) {
                                Icon(Icons.Default.DoneAll, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.s_ac210d6e7d))
                            }
                        },
                    )
                }
                item {
                    ContextHero(
                        title = if (state.summary.unread == 0) stringResource(Res.string.s_f5d9c71552) else stringResource(Res.string.s_f86d4f875a, state.summary.unread),
                        subtitle = stringResource(Res.string.s_26fdb68077, state.summary.total, state.summary.muted),
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
                        placeholder = { Text(stringResource(Res.string.s_557ee228de)) },
                        singleLine = true,
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.filter.unreadOnly,
                            onClick = { viewModel.onEvent(Event.UnreadOnlyChanged(!state.filter.unreadOnly)) },
                            label = { Text(stringResource(Res.string.filter_unread_only)) },
                        )
                        FilterChip(
                            selected = state.filter.includeMuted,
                            onClick = { viewModel.onEvent(Event.IncludeMutedChanged(!state.filter.includeMuted)) },
                            label = { Text(stringResource(Res.string.s_3c0aaf62db)) },
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
                                label = { Text(stringResource(Res.string.s_f8d22fd22e)) },
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
                        ThreadLoadingState(modifier = Modifier.fillMaxWidth())
                    }
                    is LoadState.Error -> item {
                        ThreadCard(Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.s_d37c67c75a), style = MaterialTheme.typography.titleMedium)
                            Text(refresh.error.message ?: stringResource(Res.string.s_d01a73e67b), color = MaterialTheme.colorScheme.error)
                            SaniouButton(onClick = inbox::retry, text = stringResource(Res.string.action_retry))
                        }
                    }
                    else -> if (inbox.itemCount == 0) item {
                        ThreadCard(Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.MarkEmailRead, null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(Res.string.s_ccb91c07d7), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.s_41ca4f4fa4), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = threadAnimateItem(),
                        )
                    }
                }
                item { PagingAppendState(inbox) }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun InboxMetrics(total: Int, unread: Int, sources: Int, muted: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(stringResource(Res.string.label_all) to total, stringResource(Res.string.label_unread) to unread, stringResource(Res.string.label_source) to sources, stringResource(Res.string.s_afdbd1ddec) to muted).forEach { (label, value) ->
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
    modifier: Modifier = Modifier,
) {
    ThreadCard(
        modifier = modifier.fillMaxWidth().clickable(enabled = event.reference != null, onClick = onOpen),
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
                    Icon(if (event.isRead) Icons.Default.Notifications else Icons.Default.MarkEmailRead, if (event.isRead) stringResource(Res.string.s_64d2bc34d9) else stringResource(Res.string.s_b1971f6896))
                }
                IconButton(onClick = onToggleMute) {
                    Icon(if (event.muted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, if (event.muted) stringResource(Res.string.s_c4856b8590) else stringResource(Res.string.s_1a0ea4638c))
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, stringResource(Res.string.s_5274a19b11)) }
            }
        }
    }
}

@Composable
private fun InboxKind.label(): String = when (this) {
    InboxKind.ANNOUNCEMENT -> stringResource(Res.string.label_announcement)
    InboxKind.MENTION -> stringResource(Res.string.s_9f7ab435be)
    InboxKind.REPLY -> stringResource(Res.string.action_reply)
    InboxKind.SUBSCRIPTION_UPDATE -> stringResource(Res.string.s_5319af762d)
    InboxKind.READER_UPDATE -> "Reader"
    InboxKind.SYSTEM -> stringResource(Res.string.s_1a1f6dff78)
}

private fun InboxKind.icon(): ImageVector = when (this) {
    InboxKind.ANNOUNCEMENT -> Icons.Default.Campaign
    InboxKind.MENTION -> Icons.Default.AlternateEmail
    InboxKind.REPLY -> Icons.AutoMirrored.Filled.Reply
    InboxKind.SUBSCRIPTION_UPDATE -> Icons.Default.Notifications
    InboxKind.READER_UPDATE -> Icons.Default.RssFeed
    InboxKind.SYSTEM -> Icons.Default.Info
}