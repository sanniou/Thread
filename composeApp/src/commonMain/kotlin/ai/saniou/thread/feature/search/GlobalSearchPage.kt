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
import org.jetbrains.compose.resources.stringResource
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_0dfffe9881
import thread.composeapp.generated.resources.s_2501ff4d66
import thread.composeapp.generated.resources.s_2a73ffdf99
import thread.composeapp.generated.resources.s_3b89e3b012
import thread.composeapp.generated.resources.s_61bcd04508
import thread.composeapp.generated.resources.s_6c53fc701c
import thread.composeapp.generated.resources.s_98d1a2291c
import thread.composeapp.generated.resources.s_a4330209a2
import thread.composeapp.generated.resources.s_a970323576
import thread.composeapp.generated.resources.label_local_first
import thread.composeapp.generated.resources.s_cf392f36e1
import thread.composeapp.generated.resources.s_e33f273247
import thread.composeapp.generated.resources.s_e5acde55aa
import thread.composeapp.generated.resources.label_global_discovery
import thread.composeapp.generated.resources.s_ec4bfed1fe

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
                    title = stringResource(Res.string.label_global_discovery),
                    eyebrow = stringResource(Res.string.s_cf392f36e1),
                    subtitle = stringResource(Res.string.s_61bcd04508),
                )
                ContextHero(
                    icon = Icons.Default.Search,
                    title = stringResource(Res.string.s_6c53fc701c),
                    subtitle = stringResource(Res.string.s_3b89e3b012),
                    metric = state.response?.let { stringResource(Res.string.s_98d1a2291c, it.totalCount) } ?: stringResource(Res.string.label_local_first),
                )
                ThreadCard(Modifier.fillMaxWidth()) {
                    if (state.smartCollections.isNotEmpty()) {
                        Text(stringResource(Res.string.s_e33f273247), style = MaterialTheme.typography.labelLarge)
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
                        label = { Text(stringResource(Res.string.s_ec4bfed1fe)) },
                        placeholder = { Text(stringResource(Res.string.s_a970323576)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onEvent(Event.Clear) }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.s_2a73ffdf99))
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
        title = stringResource(Res.string.s_e5acde55aa),
        description = stringResource(Res.string.s_0dfffe9881),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SearchEmptyState(query: String) {
    ModernEmptyState(
        icon = Icons.Default.SearchOff,
        title = stringResource(Res.string.s_2501ff4d66, query.trim()),
        description = stringResource(Res.string.s_a4330209a2),
        modifier = Modifier.fillMaxWidth(),
    )
}
