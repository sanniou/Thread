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
import ai.saniou.coreui.widgets.CacheStatusBanner
import ai.saniou.coreui.widgets.CacheStatusTone
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouOutlinedButton
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.KeyedLazyListState
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.SidebarHeader
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.ArticleItem as ArticleListItem
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.feature.feed.ui.components.FeedRichText
import ai.saniou.thread.domain.model.feed.ArticleItem
import ai.saniou.thread.domain.model.feed.PostItem
import ai.saniou.thread.domain.model.feed.SocialItem
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import thread.feature_feed.generated.resources.Res
import thread.feature_feed.generated.resources.s_16dbcf21a4
import thread.feature_feed.generated.resources.s_246b5aebf6
import thread.feature_feed.generated.resources.s_274a1a8d2e
import thread.feature_feed.generated.resources.s_38108eaa1d
import thread.feature_feed.generated.resources.s_3e14c19001
import thread.feature_feed.generated.resources.s_53a0111222
import thread.feature_feed.generated.resources.s_5b60fee8a5
import thread.feature_feed.generated.resources.s_65ee1f4b65
import thread.feature_feed.generated.resources.s_6aecb04c5c
import thread.feature_feed.generated.resources.s_6fd70bcc6e
import thread.feature_feed.generated.resources.s_750ec0e335
import thread.feature_feed.generated.resources.s_82fe3557c1
import thread.feature_feed.generated.resources.s_89f66ab502
import thread.feature_feed.generated.resources.s_8d5c61c2aa
import thread.feature_feed.generated.resources.s_924ce8c469
import thread.feature_feed.generated.resources.s_ae1913973c
import thread.feature_feed.generated.resources.s_b372dd3411
import thread.feature_feed.generated.resources.s_cec000d0c0
import thread.feature_feed.generated.resources.s_d1be8ec160
import thread.feature_feed.generated.resources.s_db3bdb19bb
import thread.feature_feed.generated.resources.s_e45f66052e
import thread.feature_feed.generated.resources.s_f0e21f6e0f
import thread.feature_feed.generated.resources.s_f675ecc2a1
import thread.feature_feed.generated.resources.s_f82f565394
import thread.feature_feed.generated.resources.s_ffa27be6da
import thread.feature_feed.generated.resources.s_ffc7247056

@Composable
fun UnifiedFeedPage(
    viewModel: FeedViewModel,
    onOpenTopic: (Topic) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onOpenSocial: (SocialPost) -> Unit = {},
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
                onOpenSocial = onOpenSocial,
                onListPositionChanged = { contextKey, index, offset ->
                    viewModel.onEvent(FeedContract.Event.ListPositionChanged(contextKey, index, offset))
                },
                onEvent = viewModel::onEvent,
            )
    }
}

@Composable
private fun FeedFilterDrawer(
    state: FeedContract.State,
    onEvent: (FeedContract.Event) -> Unit,
) {
    val selectionCount = state.selectedSourceIds.size +
        (if (state.includeReader) 1 else 0) +
        (if (state.includeSocial) state.selectedSocialSourceIds.size else 0)
    Column(modifier = Modifier.fillMaxSize()) {
        SidebarHeader(
            icon = Icons.Default.DynamicFeed,
            title = stringResource(Res.string.s_16dbcf21a4),
            subtitle = stringResource(Res.string.s_ffa27be6da, selectionCount),
        )

        AppDrawerItem(
            label = stringResource(Res.string.s_53a0111222),
            icon = Icons.Default.Checklist,
            selected = state.sources.isNotEmpty() && state.selectedSourceIds.size == state.sources.size,
            onClick = { onEvent(FeedContract.Event.SelectAllSources) },
        )

        if (state.socialSources.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            AppDrawerItem(
                label = stringResource(Res.string.s_5b60fee8a5),
                icon = Icons.Default.Public,
                selected = state.includeSocial,
                onClick = { onEvent(FeedContract.Event.ToggleSocial) },
                trailingContent = {
                    Checkbox(checked = state.includeSocial, onCheckedChange = null)
                },
            )
            state.socialSources.forEach { source ->
                val selected = source.id in state.selectedSocialSourceIds
                AppDrawerItem(
                    label = "${source.name} · ${source.host}",
                    icon = Icons.Default.Public,
                    selected = selected,
                    onClick = { onEvent(FeedContract.Event.ToggleSocialSource(source.id)) },
                    trailingContent = {
                        Checkbox(checked = selected, onCheckedChange = null)
                    },
                )
            }
        }
        AppDrawerItem(
            label = stringResource(Res.string.s_65ee1f4b65),
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
            label = stringResource(Res.string.s_246b5aebf6),
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
    onOpenSocial: (SocialPost) -> Unit,
    onListPositionChanged: (String, Int, Int) -> Unit,
    onEvent: (FeedContract.Event) -> Unit,
) {
    val selectionCount = state.selectedSourceIds.size +
        (if (state.includeReader) 1 else 0) +
        (if (state.includeSocial) state.selectedSocialSourceIds.size else 0)
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
                title = stringResource(Res.string.s_ae1913973c),
                subtitle = stringResource(Res.string.s_750ec0e335),
                metric = stringResource(Res.string.s_f82f565394, selectionCount),
                modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                    .padding(
                        horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                        vertical = 18.dp,
                    ),
                actions = {
                    if (showMenu) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.s_3e14c19001))
                        }
                    }
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.s_38108eaa1d))
                        }
                    }
                },
            )
            val cacheTone = when {
                state.isRefreshing -> CacheStatusTone.REFRESHING
                state.refreshFailures.isNotEmpty() -> CacheStatusTone.STALE
                else -> CacheStatusTone.CACHED
            }
            val cacheTitle = when (cacheTone) {
                CacheStatusTone.REFRESHING -> stringResource(Res.string.s_ffc7247056)
                CacheStatusTone.STALE -> stringResource(Res.string.s_e45f66052e)
                else -> stringResource(Res.string.s_f0e21f6e0f)
            }
            val cacheDetail = when (cacheTone) {
                CacheStatusTone.REFRESHING -> stringResource(Res.string.s_cec000d0c0)
                CacheStatusTone.STALE -> stringResource(Res.string.s_274a1a8d2e, state.refreshFailures.size)
                else -> stringResource(Res.string.s_89f66ab502)
            }
            CacheStatusBanner(
                title = cacheTitle,
                tone = cacheTone,
                detail = cacheDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Dimens.contentMaxWidth)
                    .padding(
                        horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                        vertical = 4.dp,
                    ),
            )
            PullToRefreshWrapper(
                isRefreshing = state.isRefreshing,
                onRefreshTrigger = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = Dimens.contentMaxWidth),
            ) {
                PagingStateLayout(
                    items = timeline,
                    modifier = Modifier.fillMaxSize(),
                    empty = {
                        FeedEmptyState(
                            hasSelection = state.selectedSourceIds.isNotEmpty() ||
                                state.includeReader ||
                                (state.includeSocial && state.selectedSocialSourceIds.isNotEmpty()),
                            hasAnySource = state.sources.isNotEmpty() || state.socialSources.isNotEmpty(),
                            onRefresh = onRefresh,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    },
                ) {
                val listStateKey = buildString {
                    append(state.selectedSourceIds.sorted().joinToString(","))
                    append(":reader=")
                    append(state.includeReader)
                    append(":social=")
                    append(state.includeSocial)
                    append(':')
                    append(state.selectedSocialSourceIds.sorted().joinToString(","))
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
                            val itemModifier = threadAnimateItem()
                            when (val item = timeline[index]) {
                                is PostItem -> TimelinePostCard(item.post, onOpenTopic, modifier = itemModifier)
                                is ArticleItem -> ArticleListItem(
                                    article = item.article,
                                    sourceName = item.sourceName,
                                    onClick = { onOpenArticle(item.article) },
                                    modifier = itemModifier,
                                )
                                is SocialItem -> TimelineSocialCard(
                                    item = item,
                                    onOpen = { onOpenSocial(item.post) },
                                    onInteract = { interaction, enabled ->
                                        onEvent(FeedContract.Event.InteractSocial(item.post, interaction, enabled))
                                    },
                                    modifier = itemModifier,
                                )
                                null -> Unit
                            }
                        }
                        item(key = "paging-append") { PagingAppendState(timeline, showEnd = false) }
                        if (state.includeSocial && state.selectedSocialSourceIds.isNotEmpty()) {
                            item(key = "load-older-social") {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    SaniouTextButton(
                                        onClick = { onEvent(FeedContract.Event.LoadOlderSocial) },
                                        text = stringResource(Res.string.s_924ce8c469),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun TimelineSocialCard(
    item: SocialItem,
    onOpen: () -> Unit,
    onInteract: (SocialInteraction, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val post = item.post
    ThreadCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            post.author.avatarUrl?.let { avatar ->
                NetworkImage(
                    imageUrl = avatar,
                    contentDescription = post.author.displayName,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(post.author.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(post.author.handle, item.sourceName).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                kotlin.time.Instant.fromEpochMilliseconds(post.createdAtEpochMillis).toRelativeTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        post.contentWarning?.let { warning ->
            Spacer(Modifier.height(10.dp))
            Text(
                warning,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(Modifier.height(10.dp))
        RichText(text = post.body, maxLines = 8, overflow = TextOverflow.Ellipsis)
        post.media.firstOrNull()?.let { media ->
            Spacer(Modifier.height(12.dp))
            NetworkImage(
                imageUrl = media.previewUrl ?: media.url,
                contentDescription = media.altText,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SocialInteractionButton(post, SocialInteraction.REPLY, Icons.Outlined.ChatBubbleOutline, onInteract)
            SocialInteractionButton(post, SocialInteraction.REPOST, Icons.Outlined.Repeat, onInteract)
            SocialInteractionButton(post, SocialInteraction.LIKE, Icons.Outlined.FavoriteBorder, onInteract)
            SocialInteractionButton(post, SocialInteraction.BOOKMARK, Icons.Outlined.BookmarkBorder, onInteract)
        }
    }
}

@Composable
private fun SocialInteractionButton(
    post: SocialPost,
    interaction: SocialInteraction,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onInteract: (SocialInteraction, Boolean) -> Unit,
) {
    if (interaction !in post.permittedInteractions) return
    val active = interaction in post.activeInteractions
    SaniouTextButton(onClick = { onInteract(interaction, !active) }) {
        Icon(
            icon,
            contentDescription = interaction.name,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        post.interactionCounts[interaction]?.takeIf { it > 0 }?.let { count ->
            Spacer(Modifier.width(4.dp))
            Text(count.toString())
        }
    }
}


@Composable
private fun TimelinePostCard(
    topic: Topic,
    onClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
) {
    ThreadCard(
        modifier = modifier.fillMaxWidth().clickable { onClick(topic) },
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
                text = stringResource(Res.string.s_82fe3557c1, topic.author.name, topic.commentCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun FeedEmptyState(
    hasSelection: Boolean,
    hasAnySource: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModernEmptyState(
        icon = Icons.Default.DynamicFeed,
        title = when {
            !hasAnySource -> stringResource(Res.string.s_b372dd3411)
            hasSelection -> stringResource(Res.string.s_6aecb04c5c)
            else -> stringResource(Res.string.s_db3bdb19bb)
        },
        description = when {
            !hasAnySource -> stringResource(Res.string.s_8d5c61c2aa)
            hasSelection -> stringResource(Res.string.s_6fd70bcc6e)
            else -> stringResource(Res.string.s_d1be8ec160)
        },
        modifier = modifier,
        action = if (hasSelection) {
            {
                SaniouButton(onClick = onRefresh, text = stringResource(Res.string.s_f675ecc2a1))
            }
        } else null,
    )
}
