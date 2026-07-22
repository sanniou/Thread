package ai.saniou.forum.workflow.home

import ai.saniou.coreui.layout.AdaptiveSidebarScaffold
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.theme.threadTweenSpec
import ai.saniou.coreui.widgets.CacheStatusBanner
import ai.saniou.coreui.widgets.CacheStatusTone
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
import ai.saniou.coreui.widgets.SaniouOutlinedButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SectionLabel
import ai.saniou.coreui.widgets.SidebarHeader
import ai.saniou.forum.workflow.home.ChannelContract.ChannelCategoryUiState
import ai.saniou.forum.workflow.home.ChannelContract.Event
import ai.saniou.forum.workflow.init.SourceInitScreen
import ai.saniou.forum.workflow.search.SearchPage
import ai.saniou.forum.workflow.subscription.SubscriptionPage
import ai.saniou.forum.workflow.source.SourceManagerPage
import ai.saniou.forum.workflow.topic.TopicPage
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.trend.TrendPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Notice
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.*
import thread.feature_forum.generated.resources.category_collapse
import thread.feature_forum.generated.resources.category_expand
import thread.feature_forum.generated.resources.drawer_search
import thread.feature_forum.generated.resources.drawer_sign_favorites
import thread.feature_forum.generated.resources.drawer_subscribe
import thread.feature_forum.generated.resources.empty_workspace_title
import thread.feature_forum.generated.resources.empty_workspace_desc
import thread.feature_forum.generated.resources.empty_workspace_add_source
import thread.feature_forum.generated.resources.empty_workspace_import
import thread.feature_forum.generated.resources.cache_forum_stale_title
import thread.feature_forum.generated.resources.cache_forum_cached_title
import thread.feature_forum.generated.resources.cache_forum_stale_detail
import thread.feature_forum.generated.resources.cache_forum_cached_detail
import thread.feature_forum.generated.resources.action_view_health
import thread.feature_forum.generated.resources.retry
import thread.feature_forum.generated.resources.label_dismiss_notice
import thread.feature_forum.generated.resources.eyebrow_forum_channel
import thread.feature_forum.generated.resources.label_community
import thread.feature_forum.generated.resources.label_announcement
import thread.feature_forum.generated.resources.label_content_sources
import thread.feature_forum.generated.resources.s_782c5b3b11
import thread.feature_forum.generated.resources.s_b5a583c5fd
import thread.feature_forum.generated.resources.s_dcdbeb9e99

data class ChannelPage(
    val drawerState: DrawerState? = null,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel: ChannelViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val actualDrawerState = drawerState ?: rememberDrawerState(DrawerValue.Closed)

        LaunchedEffect(state.toastMessage) {
            state.toastMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.onEvent(Event.ToastShown)
            }
        }

        val windowInfo = LocalThreadWindowInfo.current
        Box(Modifier.fillMaxSize()) {

            val drawerContent = @Composable {
                ForumDrawerContent(
                    state = state,
                    viewModel = viewModel,
                    navigator = navigator,
                    onCloseDrawer = {
                        if (!windowInfo.hasPermanentFeatureSidebar) {
                            scope.launch { actualDrawerState.close() }
                        }
                    },
                )
            }

            AdaptiveSidebarScaffold(
                drawerState = actualDrawerState,
                coroutineScope = scope,
                sidebar = drawerContent,
            ) { showMenu, openSidebar ->
                MainContent(state, if (showMenu) openSidebar else null)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    @Composable
    private fun MainContent(
        state: ChannelContract.ChannelUiState,
        onMenuClick: (() -> Unit)?
    ) {
        val viewModel: ChannelViewModel = rememberScreenModel()
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val updateWorkspaceSession = remember { di.direct.instance<UpdateWorkspaceSessionUseCase>() }
        val scope = rememberCoroutineScope()

        if (state.availableSources.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ModernEmptyState(
                    icon = Icons.Default.Forum,
                    title = stringResource(Res.string.empty_workspace_title),
                    description = stringResource(Res.string.empty_workspace_desc),
                    action = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SaniouButton(
                                onClick = { navigator.push(SourceManagerPage()) },
                                text = stringResource(Res.string.empty_workspace_add_source),
                            )
                            SaniouOutlinedButton(
                                onClick = {
                                    scope.launch {
                                        updateWorkspaceSession { current ->
                                            current.copy(
                                                destination = WorkspaceDestination.SETTINGS,
                                                lastContent = null,
                                                updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                                            )
                                        }
                                    }
                                },
                                text = stringResource(Res.string.empty_workspace_import),
                            )
                        }
                    },
                )
            }
            return
        }

        if (!state.isCurrentSourceInitialized) {
            SourceInitScreen(
                sourceId = state.currentSourceId,
                onInitialized = {
                    // Initialization handled by ViewModel observing flow
                }
            ).Content()
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            val cacheTone = when {
                state.refreshFailures.isNotEmpty() -> CacheStatusTone.STALE
                else -> CacheStatusTone.CACHED
            }
            val cacheTitle = when (cacheTone) {
                CacheStatusTone.STALE -> stringResource(Res.string.cache_forum_stale_title)
                else -> stringResource(Res.string.cache_forum_cached_title)
            }
            val cacheDetail = when (cacheTone) {
                CacheStatusTone.STALE -> stringResource(
                    Res.string.cache_forum_stale_detail,
                    state.refreshFailures.size,
                )
                else -> stringResource(Res.string.cache_forum_cached_detail)
            }
            CacheStatusBanner(
                title = cacheTitle,
                tone = cacheTone,
                detail = cacheDetail,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                action = {
                    if (state.refreshFailures.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SaniouTextButton(
                                onClick = { viewModel.onEvent(Event.LoadCategories) },
                                text = stringResource(Res.string.retry),
                            )
                            SaniouTextButton(
                                onClick = {
                                    scope.launch {
                                        updateWorkspaceSession { current ->
                                            current.copy(
                                                destination = WorkspaceDestination.OPERATIONS,
                                                lastContent = null,
                                                updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                                            )
                                        }
                                    }
                                },
                                text = stringResource(Res.string.action_view_health),
                            )
                        }
                    }
                },
            )

            state.notice?.let { notice ->
                NoticeBanner(
                    notice = notice,
                    onDismiss = { viewModel.onEvent(Event.MarkNoticeRead) }
                )
            }

            state.currentChannel?.let { forum ->
                TopicPage(
                    sourceId = state.currentSourceId,
                    forumId = forum.id,
                    fgroupId = forum.groupId,
                    onMenuClick = onMenuClick,
                    initialListIndex = state.listAnchor
                        ?.takeIf { it.contextKey == "${state.currentSourceId}:${forum.id}" }
                        ?.index ?: 0,
                    initialListOffset = state.listAnchor
                        ?.takeIf { it.contextKey == "${state.currentSourceId}:${forum.id}" }
                        ?.offset ?: 0,
                    onListPositionChanged = { index, offset ->
                        viewModel.onEvent(
                            Event.ListPositionChanged("${state.currentSourceId}:${forum.id}", index, offset)
                        )
                    },
                ).Content()
            } ?: run {
                TrendPage(
                    onMenuClick = onMenuClick
                ).Content()
            }
        }
    }

    @Composable
    private fun NoticeBanner(
        notice: Notice,
        onDismiss: () -> Unit
    ) {
        NoticeDisplay(
            notice = notice,
            onDismiss = onDismiss
        )
    }

    @Composable
    private fun ForumDrawerContent(
        state: ChannelContract.ChannelUiState,
        viewModel: ChannelViewModel,
        navigator: Navigator,
        onCloseDrawer: () -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val currentSource = state.availableSources.firstOrNull { it.id == state.currentSourceId }
            SidebarHeader(
                icon = Icons.Default.Forum,
                title = stringResource(Res.string.label_community),
                subtitle = currentSource?.name ?: stringResource(Res.string.s_b5a583c5fd),
            )

            SectionLabel(
                text = stringResource(Res.string.label_content_sources),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            AnimatedSourceSelector(
                sources = state.availableSources,
                currentSourceId = state.currentSourceId,
                onSourceSelected = { viewModel.onEvent(Event.SelectSource(it)) }
            )

            DrawerFunctionGrid(
                state = state,
                navigator = navigator,
                viewModel = viewModel,
                onCloseDrawer = onCloseDrawer
            )

            RefreshDiagnosticsBanner(
                failures = state.refreshFailures,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                onRetry = { viewModel.onEvent(Event.LoadCategories) },
            )

            SectionLabel(
                text = stringResource(Res.string.eyebrow_forum_channel),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            StateLayout(
                state = state.categoriesState,
                onRetry = { viewModel.onEvent(Event.LoadCategories) },
                modifier = Modifier.weight(1f)
            ) { forumGroups ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    forumGroups.forEach { group ->
                        item(key = group.id) {
                            CategoryHeader(
                                group = group,
                                isExpanded = state.expandedGroupId == group.id,
                                onToggle = { viewModel.onEvent(Event.ToggleCategory(group.id)) }
                            )
                        }

                        item(key = "content_${group.id}") {
                            AnimatedVisibility(
                                visible = state.expandedGroupId == group.id,
                                enter = expandVertically(animationSpec = threadTweenSpec()) +
                                    fadeIn(animationSpec = threadTweenSpec()),
                                exit = shrinkVertically(animationSpec = threadTweenSpec()) +
                                    fadeOut(animationSpec = threadTweenSpec()),
                            ) {
                                Column {
                                    group.channels.forEach { forum ->
                                        StylizedForumItem(
                                            forum = forum,
                                            isSelected = state.currentChannel?.id == forum.id,
                                            isFavorite = state.favoriteChannelIds.contains(forum.id),
                                            onForumClick = {
                                                viewModel.onEvent(Event.SelectChannel(forum))
                                                onCloseDrawer()
                                            },
                                            onFavoriteToggle = { viewModel.onEvent(Event.ToggleFavorite(forum)) }
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun DrawerFunctionGrid(
        state: ChannelContract.ChannelUiState,
        navigator: Navigator,
        viewModel: ChannelViewModel,
        onCloseDrawer: () -> Unit
    ) {
        val items = buildList {
            add(DrawerItemData(stringResource(Res.string.s_dcdbeb9e99), Icons.Default.Home, state.currentChannel == null) {
                viewModel.onEvent(Event.SelectHome)
                onCloseDrawer()
            })
            add(DrawerItemData(stringResource(Res.string.drawer_subscribe), Icons.Default.Favorite, false) {
                navigator.push(SubscriptionPage { threadId -> navigator.push(TopicDetailPage(threadId)) })
                onCloseDrawer()
            })
            val currentSource = state.availableSources.firstOrNull { it.id == state.currentSourceId }
            if (currentSource?.capabilities?.supportsSearch == true) {
                add(DrawerItemData(stringResource(Res.string.drawer_search), Icons.Default.Search, false) {
                    navigator.push(SearchPage(state.currentSourceId))
                    onCloseDrawer()
                })
            }
            if (currentSource?.capabilities?.supportsChannelSign == true) {
                add(DrawerItemData(stringResource(Res.string.drawer_sign_favorites), Icons.Default.CheckCircle, false) {
                    viewModel.onEvent(Event.SignFavorites)
                    onCloseDrawer()
                })
            }
            add(DrawerItemData(stringResource(Res.string.s_782c5b3b11), Icons.Default.Settings, false) {
                navigator.push(SourceManagerPage())
                onCloseDrawer()
            })
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                DrawerGridItem(
                    label = item.label,
                    icon = item.icon,
                    selected = item.selected,
                    onClick = item.onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    private data class DrawerItemData(
        val label: String,
        val icon: ImageVector,
        val selected: Boolean,
        val onClick: () -> Unit
    )

    @Composable
    private fun DrawerGridItem(
        label: String,
        icon: ImageVector,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val backgroundColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

        val contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = backgroundColor,
            border = BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
          Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
          }
        }
    }

    @Composable
    private fun CategoryHeader(
        group: ChannelCategoryUiState,
        isExpanded: Boolean,
        onToggle: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) stringResource(Res.string.category_collapse) else stringResource(
                    Res.string.category_expand
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun NoticeDisplay(notice: Notice, onDismiss: () -> Unit) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(Res.string.label_announcement),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                RichText(
                    text = notice.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SaniouTextButton(onClick = onDismiss, text = stringResource(Res.string.label_dismiss_notice))
                }
            }
        }
    }
}
