package ai.saniou.forum.workflow.home

import ai.saniou.coreui.composition.LocalAppDrawer
import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.widgets.DrawerHeader
import ai.saniou.coreui.widgets.RichText
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.workflow.home.ChannelContract.ChannelCategoryUiState
import ai.saniou.forum.workflow.home.ChannelContract.Event
import ai.saniou.forum.workflow.init.SourceInitScreen
import ai.saniou.forum.workflow.search.SearchPage
import ai.saniou.forum.workflow.subscription.SubscriptionPage
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import org.kodein.di.DI
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.*
import thread.feature_forum.generated.resources.category_collapse
import thread.feature_forum.generated.resources.category_expand
import thread.feature_forum.generated.resources.drawer_search
import thread.feature_forum.generated.resources.drawer_subscribe

data class ChannelPage(
    val di: DI = nmbdi,
    val drawerState: DrawerState? = null,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel: ChannelViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val greetImageViewModel: GreetImageViewModel = rememberScreenModel()
        val greetImageUrl by greetImageViewModel.greetImageUrl.collectAsStateWithLifecycle()

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

        BoxWithConstraints {
            val isMobile = maxWidth < ai.saniou.coreui.theme.Dimens.MobileWidth

            val drawerContent = @Composable {
                ForumDrawerContent(
                    greetImageUrl = greetImageUrl,
                    state = state,
                    viewModel = viewModel,
                    navigator = navigator,
                    onCloseDrawer = {
                        if (isMobile) {
                            scope.launch { actualDrawerState.close() }
                        }
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            val onMenuClick = {
                scope.launch { actualDrawerState.open() }
                Unit
            }

            if (isMobile) {
                ModalNavigationDrawer(
                    drawerState = actualDrawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            drawerContentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            drawerContent()
                        }
                    }
                ) {
                    MainContent(state, onMenuClick)
                }
            } else {
                PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet(
                            modifier = Modifier.width(300.dp),
                            drawerContainerColor = Color.Transparent,
                            drawerContentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            drawerContent()
                        }
                    }
                ) {
                    MainContent(state, onMenuClick)
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        state: ChannelContract.ChannelUiState,
        onMenuClick: () -> Unit
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
        greetImageUrl: String?,
        state: ChannelContract.ChannelUiState,
        viewModel: ChannelViewModel,
        navigator: Navigator,
        onCloseDrawer: () -> Unit,
        snackbarHostState: SnackbarHostState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full screen background image
            DrawerHeader(
                imageUrl = greetImageUrl,
                modifier = Modifier.fillMaxSize()
            )

            GlassmorphicDrawerContainer(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Transparent spacer to reveal the image header
                    Spacer(modifier = Modifier.height(ai.saniou.coreui.theme.Dimens.drawer_header_height))

                    // List content
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val globalDrawer = LocalAppDrawer.current
                        globalDrawer()
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        AnimatedSourceSelector(
                            sources = state.availableSources,
                            currentSourceId = state.currentSourceId,
                            onSourceSelected = { viewModel.onEvent(Event.SelectSource(it)) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DrawerFunctionGrid(
                            state = state,
                            navigator = navigator,
                            viewModel = viewModel,
                            onCloseDrawer = onCloseDrawer
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        StateLayout(
                            state = state.categoriesState,
                            onRetry = { viewModel.onEvent(Event.LoadCategories) },
                            modifier = Modifier.weight(1f)
                        ) { forumGroups ->
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                forumGroups.forEach { group ->
                                    item(key = group.id) {
                                        CategoryHeader(
                                            group = group,
                                            isExpanded = state.expandedGroupId == group.id,
                                            onToggle = {
                                                viewModel.onEvent(
                                                    Event.ToggleCategory(group.id)
                                                )
                                            }
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
                                                        onFavoriteToggle = {
                                                            viewModel.onEvent(Event.ToggleFavorite(forum))
                                                        }
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
        val items = listOf(
            DrawerItemData("综合趋势", Icons.Default.Home, state.currentChannel == null) {
                viewModel.onEvent(Event.SelectHome)
                onCloseDrawer()
            },
            DrawerItemData(stringResource(Res.string.drawer_subscribe), Icons.Default.Favorite, false) {
                navigator.push(SubscriptionPage { threadId -> navigator.push(TopicDetailPage(threadId)) })
                onCloseDrawer()
            },
            DrawerItemData(stringResource(Res.string.drawer_search), Icons.Default.Search, false) {
                navigator.push(SearchPage())
                onCloseDrawer()
            }
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            maxItemsInEachRow = 3,
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
        val backgroundColor = if (selected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            Color.Transparent

        val contentColor = if (selected)
            MaterialTheme.colorScheme.onSecondaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
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
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
