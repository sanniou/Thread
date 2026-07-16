package ai.saniou.forum.workflow.home

import ai.saniou.coreui.layout.AdaptiveSidebarScaffold
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.RefreshDiagnosticsBanner
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
import ai.saniou.thread.domain.model.forum.Notice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import thread.feature_forum.generated.resources.drawer_subscribe

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
                    onMenuClick = onMenuClick
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
                title = "社区",
                subtitle = currentSource?.name ?: "选择内容来源",
            )

            SectionLabel(
                text = "内容来源",
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
                text = "版块",
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
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
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
            add(DrawerItemData("综合趋势", Icons.Default.Home, state.currentChannel == null) {
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
            add(DrawerItemData("来源管理", Icons.Default.Settings, false) {
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
        val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer

        val contentColor = if (selected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant

        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = backgroundColor,
        ) {
          Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "公告",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Button(onClick = onDismiss) {
                        Text("不再显示")
                    }
                }
            }
        }
    }
}
