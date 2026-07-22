package ai.saniou.forum.workflow.trend

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.trend.TrendContract.Effect
import ai.saniou.forum.workflow.trend.TrendContract.Event
import ai.saniou.thread.domain.model.TrendItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.eyebrow_forum_trend
import thread.feature_forum.generated.resources.post_page_back
import thread.feature_forum.generated.resources.refresh
import thread.feature_forum.generated.resources.not_interested
import thread.feature_forum.generated.resources.s_2af1650958
import thread.feature_forum.generated.resources.s_56edba4cd0
import thread.feature_forum.generated.resources.s_8e6904cf83
import thread.feature_forum.generated.resources.s_a416165e7f
import thread.feature_forum.generated.resources.s_a5db624e92
import thread.feature_forum.generated.resources.s_de25657601
import thread.feature_forum.generated.resources.s_dede7b4ef6

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

        ThreadDetailScaffold(
            title = state.selectedSource?.name ?: stringResource(Res.string.eyebrow_forum_trend),
            eyebrow = stringResource(Res.string.eyebrow_forum_trend),
            subtitle = if (state.selectedTab?.supportsHistory == true) {
                if (state.trendParams.dayOffset == 0) stringResource(Res.string.s_a5db624e92, state.selectedTab?.name.orEmpty())
                else stringResource(Res.string.s_56edba4cd0, state.trendParams.dayOffset, state.selectedTab?.name.orEmpty())
            } else {
                state.selectedTab?.name ?: stringResource(Res.string.s_8e6904cf83)
            },
            onBack = navigator::pop,
            navigationIcon = {
                if (onMenuClick != null) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.s_2af1650958))
                    }
                } else {
                    IconButton(onClick = navigator::pop) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.post_page_back))
                    }
                }
            },
            actions = {
                if (state.availableSources.size > 1) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(Res.string.s_de25657601))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            state.availableSources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source.name) },
                                    onClick = {
                                        viewModel.onEvent(Event.SelectSource(source.id))
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (state.selectedTab?.supportsHistory == true) {
                    IconButton(onClick = { viewModel.onEvent(Event.SelectDate(state.trendParams.dayOffset + 1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.s_dede7b4ef6))
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(Event.SelectDate(state.trendParams.dayOffset - 1)) },
                        enabled = state.trendParams.dayOffset > 0,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.s_a416165e7f))
                    }
                } else {
                    IconButton(onClick = { viewModel.onEvent(Event.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding).fillMaxSize()) {
                if (state.availableTabs.size > 1) {
                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                            .align(Alignment.CenterHorizontally),
                    ) {
                        state.availableTabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { viewModel.onEvent(Event.SelectTab(tab.id)) },
                                text = { Text(tab.name) },
                            )
                        }
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val tab = state.availableTabs.getOrNull(page)
                    if (tab != null) {
                        TrendList(
                            viewModel = viewModel,
                            supportsNotInterested = tab.supportsNotInterested,
                            onItemClick = { viewModel.onEvent(Event.OnTrendItemClick(it)) },
                            onNotInterested = { viewModel.onEvent(Event.NotInterested(it)) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun TrendList(
        viewModel: TrendViewModel,
        supportsNotInterested: Boolean,
        onItemClick: (TrendItem) -> Unit,
        onNotInterested: (TrendItem) -> Unit,
    ) {
        val items = viewModel.trendPagingFlow.collectAsLazyPagingItems()
        val uiState by viewModel.state.collectAsState()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            PagingStateLayout(
                items = items,
                onRetry = { items.retry() },
                modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = Dimens.page_horizontal,
                        vertical = Dimens.page_vertical,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                supportsNotInterested = supportsNotInterested,
                                notInterestedInFlight = item.topicId in uiState.notInterestedInFlight,
                                onClick = { onItemClick(item) },
                                onNotInterested = { onNotInterested(item) },
                                modifier = threadAnimateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TrendItemCard(
        index: Int,
        item: TrendItem,
        supportsNotInterested: Boolean = false,
        notInterestedInFlight: Boolean = false,
        onClick: () -> Unit,
        onNotInterested: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (item.rank != null) {
                    RankBadge(
                        index = index,
                        rank = item.rank.toString().padStart(2, '0'),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (item.channel != null) {
                            Text(
                                text = item.channel!!,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (item.isNew) {
                            Text(
                                text = "新",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (item.hotness != null) {
                            Text(
                                text = item.hotness!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (item.title.isNullOrEmpty().not() && !item.title!!.startsWith("No.")) {
                        Text(
                            text = item.title!!,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (!item.contentPreview.isNullOrBlank()) {
                        RichText(
                            text = item.contentPreview ?: "",
                            blankLinePolicy = BlankLinePolicy.REMOVE,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (supportsNotInterested) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            SaniouTextButton(
                                onClick = onNotInterested,
                                enabled = !notInterestedInFlight,
                                text = stringResource(Res.string.not_interested),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RankBadge(
        index: Int,
        rank: String,
    ) {
        val (container, content) = when (index) {
            0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            1 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            2 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            color = container.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = rank,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                modifier = Modifier
                    .width(40.dp)
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
