package ai.saniou.feature.feed.workflow

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.composition.LocalAppDrawer
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AppDrawerItem
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
import ai.saniou.coreui.widgets.ArticleItem as ArticleListItem
import ai.saniou.feature.feed.ui.components.FeedRichText
import ai.saniou.thread.domain.model.feed.ArticleItem
import ai.saniou.thread.domain.model.feed.PostItem
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.reader.Article
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
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

    BoxWithConstraints {
        val isMobile = maxWidth < Dimens.MobileWidth
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val drawerContent = @Composable {
            FeedFilterDrawer(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }
        val content = @Composable {
            FeedScaffold(
                state = state,
                timeline = timeline,
                snackbarHostState = snackbarHostState,
                showMenu = isMobile,
                onMenu = { scope.launch { drawerState.open() } },
                onRefresh = { viewModel.onEvent(FeedContract.Event.Refresh) },
                onOpenTopic = onOpenTopic,
                onOpenArticle = onOpenArticle,
            )
        }

        if (isMobile) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = { ModalDrawerSheet { drawerContent() } },
                content = content,
            )
        } else {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(288.dp)) {
                        drawerContent()
                    }
                },
                content = content,
            )
        }
    }
}

@Composable
private fun FeedFilterDrawer(
    state: FeedContract.State,
    onEvent: (FeedContract.Event) -> Unit,
) {
    val globalDrawer = LocalAppDrawer.current
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.DynamicFeed, contentDescription = null)
            Text("聚合范围", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

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
                        onCheckedChange = { onEvent(FeedContract.Event.ToggleSource(source.id)) },
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
                    onCheckedChange = { onEvent(FeedContract.Event.ToggleReader) },
                )
            },
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        globalDrawer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("聚合信息流") },
                navigationIcon = {
                    if (showMenu) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "筛选来源")
                        }
                    }
                },
                actions = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                },
            )
        },
    ) { padding ->
        PagingStateLayout(
            items = timeline,
            modifier = Modifier.padding(padding).fillMaxSize(),
            empty = {
                FeedEmptyState(
                    hasSelection = state.selectedSourceIds.isNotEmpty() || state.includeReader,
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.refreshFailures.isNotEmpty()) {
                    item {
                        RefreshDiagnosticsBanner(
                            failures = state.refreshFailures,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(2.dp)) }
                items(timeline.itemCount) { index ->
                    when (val item = timeline[index]) {
                        is PostItem -> TimelinePostCard(item.post, onOpenTopic)
                        is ArticleItem -> ArticleListItem(
                            article = item.article,
                            sourceName = item.sourceName,
                            onClick = { onOpenArticle(item.article) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        null -> Unit
                    }
                }
                when (timeline.loadState.append) {
                    is LoadState.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is LoadState.Error -> item {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("加载更多失败", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = timeline::retry) { Text("重试") }
                        }
                    }
                    else -> Unit
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun TimelinePostCard(
    topic: Topic,
    onClick: (Topic) -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().clickable { onClick(topic) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
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
}

@Composable
private fun FeedEmptyState(
    hasSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Default.DynamicFeed,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            if (hasSelection) "暂时没有时间线内容" else "请选择至少一个论坛或阅读器来源",
            style = MaterialTheme.typography.titleMedium,
        )
        if (hasSelection) {
            Text(
                "可以刷新来源，或在左侧调整聚合范围",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
