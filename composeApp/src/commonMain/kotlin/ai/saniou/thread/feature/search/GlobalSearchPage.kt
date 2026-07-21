package ai.saniou.thread.feature.search

import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.PageHeader
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadLoadingState
import ai.saniou.coreui.composition.LocalContentLinkHandler
import ai.saniou.thread.FeedTopicRoute
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.feature.search.GlobalSearchContract.Event
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import androidx.paging.compose.collectAsLazyPagingItems

object GlobalSearchPage : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel { di.direct.instance<GlobalSearchViewModel>() }
        val state by viewModel.state.collectAsState()
        val collectionResults = viewModel.collectionResults.collectAsLazyPagingItems()
        val snackbar = remember { SnackbarHostState() }
        val contentLinkHandler = LocalContentLinkHandler.current

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is GlobalSearchContract.Effect.OpenResult -> when (effect.result.type) {
                        GlobalSearchType.ARTICLE -> navigator.push(ArticleDetailPage(effect.result.id))
                        GlobalSearchType.TOPIC -> navigator.push(FeedTopicRoute(effect.result.sourceId, effect.result.id))
                        GlobalSearchType.COMMENT -> navigator.push(
                            FeedTopicRoute(effect.result.sourceId, effect.result.contextId ?: effect.result.id)
                        )
                        GlobalSearchType.SOCIAL -> contentLinkHandler?.invoke(
                            "thread://social/${effect.result.sourceId}/${effect.result.id}"
                        )
                    }
                }
            }
        }
        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.onEvent(Event.MessageShown)
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth)
                    .padding(
                        horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                        vertical = Dimens.page_vertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PageHeader(
                    title = "全局发现",
                    eyebrow = "本地搜索",
                    subtitle = "跨社区主题、回复与 Reader 正文搜索本地缓存",
                )
                ContextHero(
                    icon = Icons.Default.Search,
                    title = "一个入口，三类内容",
                    subtitle = "搜索不依赖当前来源和网络状态，结果直接回到原始阅读上下文。",
                    metric = state.response?.let { "${it.totalCount} 条命中" } ?: "本地优先",
                )
                ThreadCard(Modifier.fillMaxWidth()) {
                    if (state.smartCollections.isNotEmpty()) {
                        Text("智能集合", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.smartCollections.forEach { collection ->
                                FilterChip(
                                    selected = state.activeCollectionId == collection.id,
                                    onClick = {
                                        viewModel.onEvent(Event.ApplyCollection(
                                            collection.id.takeUnless { state.activeCollectionId == collection.id }
                                        ))
                                    },
                                    label = { Text(collection.name) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onEvent(Event.QueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("搜索缓存内容") },
                        placeholder = { Text("至少输入两个字符") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onEvent(Event.Clear) }) {
                                    Icon(Icons.Default.Close, contentDescription = "清空搜索")
                                }
                            }
                        },
                        singleLine = true,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlobalSearchType.entries.forEach { type ->
                            val presentation = type.presentation()
                            FilterChip(
                                selected = type in state.selectedTypes,
                                onClick = { viewModel.onEvent(Event.TypeToggled(type)) },
                                label = { Text(presentation.label) },
                                leadingIcon = { Icon(presentation.icon, null) },
                                enabled = type !in state.selectedTypes || state.selectedTypes.size > 1,
                            )
                        }
                    }
                }
                when {
                    state.activeCollectionId != null -> GlobalSearchPagingResults(
                        results = collectionResults,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onOpen = viewModel::open,
                    )
                    state.isSearching -> ThreadLoadingState(Modifier.align(Alignment.CenterHorizontally))
                    state.activeCollectionId == null && state.query.trim().length < 2 -> SearchGuidance()
                    state.response?.results.isNullOrEmpty() -> SearchEmptyState(state.query)
                    else -> GlobalSearchResults(
                        results = state.response!!.results,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onOpen = viewModel::open,
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun SearchGuidance() {
    ModernEmptyState(
        icon = Icons.Default.Search,
        title = "从已有内容开始",
        description = "缓存搜索覆盖已加载的论坛主题、回复和 Reader 文章；即使某个来源暂时离线，已经阅读过的内容仍可发现。",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SearchEmptyState(query: String) {
    ModernEmptyState(
        icon = Icons.Default.SearchOff,
        title = "没有找到“${query.trim()}”",
        description = "可以缩短关键词、启用更多内容类型，或先刷新对应来源。",
        modifier = Modifier.fillMaxWidth(),
    )
}
