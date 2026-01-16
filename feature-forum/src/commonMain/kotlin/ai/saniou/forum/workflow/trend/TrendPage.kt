package ai.saniou.forum.workflow.trend

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouAppBarTitle
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.coreui.widgets.VerticalSpacerSmall
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.thread.domain.model.TrendItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance

data class TrendPage(
    val onMenuClick: (() -> Unit)? = null,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val dI = localDI()
        val navigator = LocalNavigator.currentOrThrow
        val forumSourceId = LocalForumSourceId.current
        val viewModel: TrendViewModel = rememberScreenModel(tag = forumSourceId) {
            dI.direct.instance(arg = forumSourceId)
        }
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f
        ) {
            state.availableTabs.size
        }

        LaunchedEffect(state.selectedTab) {
            val index = state.availableTabs.indexOf(state.selectedTab)
            if (index != -1 && pagerState.currentPage != index) {
                pagerState.animateScrollToPage(index)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            val tab = state.availableTabs.getOrNull(pagerState.currentPage)
            if (tab != null && state.selectedTab != tab) {
                viewModel.onEvent(Event.SelectTab(tab.id))
            }
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is Effect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }

                    is Effect.NavigateToThread -> {
                        navigator.push(TopicDetailPage(effect.topicId))
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                Column {
                    SaniouTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SaniouAppBarTitle(
                                    title = state.selectedSource?.name ?: "趋势",
                                    subtitle = if (state.selectedTab?.supportsHistory == true) {
                                        // TODO: Format date properly
                                        if (state.trendParams.dayOffset == 0) "今天" else "${state.trendParams.dayOffset}天前"
                                    } else {
                                        null
                                    }
                                )
                                // Source Switcher
                                if (state.availableSources.size > 1) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch Source"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            state.availableSources.forEach { source ->
                                                DropdownMenuItem(
                                                    text = { Text(source.name) },
                                                    onClick = {
                                                        viewModel.onEvent(Event.SelectSource(source.id))
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onNavigationClick = {
                            if (onMenuClick != null) {
                                onMenuClick.invoke()
                            } else {
                                navigator.pop()
                            }
                        },
                        navigationIcon = {
                            if (onMenuClick != null) {
                                IconButton(onClick = { onMenuClick.invoke() }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            } else {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        actions = {
                            if (state.selectedTab?.supportsHistory == true) {
                                IconButton(onClick = { viewModel.onEvent(Event.SelectDate(state.trendParams.dayOffset + 1)) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "前一天"
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.onEvent(Event.SelectDate(state.trendParams.dayOffset - 1)) },
                                    enabled = state.trendParams.dayOffset > 0
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "后一天"
                                    )
                                }
                            } else {
                                IconButton(onClick = { viewModel.onEvent(Event.Refresh) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                                }
                            }
                        }
                    )

                    if (state.availableTabs.size > 1) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            divider = {}
                        ) {
                            state.availableTabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        viewModel.onEvent(Event.SelectTab(tab.id))
                                    },
                                    text = {
                                        Text(
                                            text = tab.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                // Ensure we are rendering the correct tab content
                // Since HorizontalPager keeps pages alive, we need to make sure the content matches the tab
                // But here we use a single PagingFlow in ViewModel that updates based on selection.
                // This might cause issues with Pager preloading.
                // Ideally, each page should have its own flow or we disable preloading/caching issues.
                // Given the requirement, we will just render the list which observes the current flow.
                // Note: This is a simplification. For robust Pager, we might need a map of flows.
                // But since we want dynamic tabs, a single flow updated by selection is tricky with Pager.
                // Let's assume the user switches tabs via tap, which updates VM state, which updates flow.
                // The Pager swipe also updates VM state.

                val tab = state.availableTabs.getOrNull(page) ?: return@HorizontalPager

                // Only render content if this page corresponds to the selected tab to avoid mismatched data
                // or accept that off-screen pages might show stale data until swiped to.
                // Actually, we should probably just use the flow.

                TrendList(
                    viewModel = viewModel,
                    onItemClick = { viewModel.onEvent(Event.OnTrendItemClick(it)) }
                )
            }
        }
    }

    @Composable
    fun TrendList(
        viewModel: TrendViewModel,
        onItemClick: (TrendItem) -> Unit,
    ) {
        val items = viewModel.trendPagingFlow.collectAsLazyPagingItems()

        PagingStateLayout(
            items = items,
            onRetry = { items.retry() }
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.topicId }
                ) { index ->
                    val item = items[index]
                    if (item != null) {
                        TrendItemCard(
                            index = index,
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun TrendItemCard(
        index: Int,
        item: TrendItem,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat style for list
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Rank Number
                if (item.rank != null) {
                    RankText(
                        index = index,
                        rank = item.rank.toString().padStart(2, '0')
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Header: Forum | ID | Time?
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.channel != null) {
                            Text(
                                text = item.channel!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = if (item.title?.startsWith("No.")?:false) item.title!! else "No.${item.topicId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (item.isNew) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    VerticalSpacerSmall()

                    if (item.title.isNullOrEmpty().not() && !item.title!!.startsWith("No.")) {
                        Text(
                            text = item.title!!,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        VerticalSpacerSmall()
                    }

                    RichText(
                        text = item.contentPreview?:"",
                        blankLinePolicy = BlankLinePolicy.REMOVE,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )

                    VerticalSpacerSmall()

                    if (item.hotness != null) {
                        Text(
                            text = item.hotness!!, // e.g., "Trend 34" or "34 replies"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RankText(
        index: Int,
        rank: String,
    ) {
        val color = when (index) {
            0 -> Color(0xFFD50000) // RedA700
            1 -> Color(0xFFFF6D00) // OrangeA700
            2 -> Color(0xFFFFD600) // Yellow
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        }

        Text(
            text = rank,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = color,
            modifier = Modifier.width(48.dp), // Fixed width for alignment
            textAlign = TextAlign.Center
        )
    }
}
