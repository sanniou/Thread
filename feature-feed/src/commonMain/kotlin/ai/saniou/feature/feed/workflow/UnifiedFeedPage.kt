package ai.saniou.feature.feed.workflow

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.interaction.ThreadShortcut
import ai.saniou.coreui.interaction.threadShortcutHost
import ai.saniou.coreui.layout.AdaptiveSidebarScaffold
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AppDrawerItem
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.KeyedLazyListState
import ai.saniou.coreui.widgets.SidebarHeader
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ArticleItem as ArticleListItem
import ai.saniou.feature.feed.ui.components.FeedRichText
import ai.saniou.thread.domain.model.feed.ArticleItem
import ai.saniou.thread.domain.model.feed.PostItem
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.reader.Article
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch

@Composable
fun UnifiedFeedPage(
    viewModel: FeedViewModel,
    onOpenTopic: (Topic) -> Unit,
    onOpenArticle: (Article) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val timeline = viewModel.timeline.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                FeedContract.Effect.RefreshPaging -> timeline.refresh()
            }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(FeedContract.Event.MessageShown)
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerContent = @Composable {
            FeedFilterDrawer(
                state = state,
                onEvent = viewModel::onEvent,
            )
    }
    AdaptiveSidebarScaffold(
        modifier = Modifier.threadShortcutHost(
            ThreadShortcut(Key.R) { viewModel.onEvent(FeedContract.Event.Refresh) },
        ),
        drawerState = drawerState,
        coroutineScope = scope,
        sidebar = drawerContent,
    ) { showMenu, openSidebar ->
            FeedScaffold(
                state = state,
                timeline = timeline,
                snackbarHostState = snackbarHostState,
                showMenu = showMenu,
                onMenu = openSidebar,
                onRefresh = { viewModel.onEvent(FeedContract.Event.Refresh) },
                onOpenTopic = onOpenTopic,
                onOpenArticle = onOpenArticle,
                onListPositionChanged = { contextKey, index, offset ->
                    viewModel.onEvent(FeedContract.Event.ListPositionChanged(contextKey, index, offset))
                },
            )
    }
}

@Composable
private fun FeedFilterDrawer(
    state: FeedContract.State,
    onEvent: (FeedContract.Event) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SidebarHeader(
            icon = Icons.Default.DynamicFeed,
            title = "聚合动态",
            subtitle = "${state.selectedSourceIds.size + if (state.includeReader) 1 else 0} 个内容范围",
        )

        AppDrawerItem(
            label = "全部论坛来源",
            icon = Icons.Default.Checklist,
            selected = state.sources.isNotEmpty() && state.selectedSourceIds.size == state.sources.size,
            onClick = { onEvent(FeedContract.Event.SelectAllSources) },
        )
        AppDrawerItem(
            label = "仅保留阅读器",
            icon = Icons.Default.ClearAll,
            selected = state.selectedSourceIds.isEmpty(),
            onClick = { onEvent(FeedContract.Event.ClearForumSources) },
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        state.sources.forEach { source ->
            val selected = source.id in state.selectedSourceIds
            AppDrawerItem(
                label = source.name,
                icon = Icons.Default.Forum,
                selected = selected,
                onClick = { onEvent(FeedContract.Event.ToggleSource(source.id)) },
                trailingContent = {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                    )
                },
            )
        }
        AppDrawerItem(
            label = "RSS / Atom 阅读器",
            icon = Icons.Default.RssFeed,
            selected = state.includeReader,
            onClick = { onEvent(FeedContract.Event.ToggleReader) },
            trailingContent = {
                Checkbox(
                    checked = state.includeReader,
                    onCheckedChange = null,
                )
            },
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun FeedScaffold(
    state: FeedContract.State,
    timeline: LazyPagingItems<TimelineItem>,
    snackbarHostState: SnackbarHostState,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onListPositionChanged: (String, Int, Int) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContextHero(
                icon = Icons.Default.DynamicFeed,
                title = "聚合信息流",
                subtitle = "论坛讨论与订阅文章，合并为一条可控时间线",
                metric = "${state.selectedSourceIds.size + if (state.includeReader) 1 else 0} 个范围",
                modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                    .padding(
                        horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                        vertical = 18.dp,
                    ),
                actions = {
                    if (showMenu) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "筛选来源")
                        }
                    }
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                },
            )
            PagingStateLayout(
                items = timeline,
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = Dimens.contentMaxWidth),
                empty = {
                    FeedEmptyState(
                        hasSelection = state.selectedSourceIds.isNotEmpty() || state.includeReader,
                        modifier = Modifier.align(Alignment.Center),
                    )
                },
            ) {
                val listStateKey = buildString {
                    append(state.selectedSourceIds.sorted().joinToString(","))
                    append(":reader=")
                    append(state.includeReader)
                }
                val restoredAnchor = state.listAnchor?.takeIf { it.contextKey == listStateKey }
                KeyedLazyListState(
                    stateKey = listStateKey,
                    initialIndex = restoredAnchor?.index ?: 0,
                    initialOffset = restoredAnchor?.offset ?: 0,
                    onPositionChanged = { index, offset ->
                        onListPositionChanged(listStateKey, index, offset)
                    },
                ) { listState ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                            vertical = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.refreshFailures.isNotEmpty()) {
                            item(key = "refresh-diagnostics") {
                                RefreshDiagnosticsBanner(
                                    failures = state.refreshFailures,
                                    onRetry = onRefresh,
                                )
                            }
                        }
                        items(
                            count = timeline.itemCount,
                            key = timeline.itemKey { it.uniqueId },
                        ) { index ->
                            when (val item = timeline[index]) {
                                is PostItem -> TimelinePostCard(item.post, onOpenTopic)
                                is ArticleItem -> ArticleListItem(
                                    article = item.article,
                                    sourceName = item.sourceName,
                                    onClick = { onOpenArticle(item.article) },
                                )
                                null -> Unit
                            }
                        }
                        item(key = "paging-append") { PagingAppendState(timeline, showEnd = false) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelinePostCard(
    topic: Topic,
    onClick: (Topic) -> Unit,
) {
    ThreadCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick(topic) },
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topic.sourceName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(" · ${topic.channelName}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    topic.createdAt.toRelativeTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            topic.title?.takeIf { it.isNotBlank() }?.let { title ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            FeedRichText(
                text = topic.summary?.takeIf { it.isNotBlank() } ?: topic.content,
                sourceId = topic.sourceId,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${topic.author.name} · ${topic.commentCount} 条回复",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun FeedEmptyState(
    hasSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    ModernEmptyState(
        icon = Icons.Default.DynamicFeed,
        title = if (hasSelection) "时间线暂时为空" else "还没有选择内容范围",
        description = if (hasSelection) "刷新来源，或在左侧调整聚合范围。" else "从左侧选择论坛或阅读器来源。",
        modifier = modifier,
    )
}
