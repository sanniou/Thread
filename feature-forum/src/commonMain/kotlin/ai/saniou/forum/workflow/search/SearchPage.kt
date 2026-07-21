package ai.saniou.forum.workflow.search

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadCommandBar
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ThreadFilterBar
import ai.saniou.coreui.widgets.ThreadSearchField
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.search.SearchContract.Event
import ai.saniou.forum.workflow.search.SearchContract.SearchType
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.direct
import org.kodein.di.instance
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.eyebrow_discovery
data class SearchPage(
    val sourceId: String,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = org.kodein.di.compose.localDI()
        val viewModel: SearchViewModel = rememberScreenModel(tag = sourceId) {
            di.direct.instance<String, SearchViewModel>(arg = sourceId)
        }
        val state by viewModel.state.collectAsStateWithLifecycle()

        ThreadDetailScaffold(
            title = "社区搜索",
            eyebrow = stringResource(Res.string.eyebrow_discovery),
            subtitle = "在当前来源的主题与回复中检索 · $sourceId",
            onBack = navigator::pop,
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ThreadCommandBar(
                    modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                            vertical = 16.dp,
                        ),
                    primary = {
                        ThreadSearchField(
                            query = state.query,
                            onQueryChange = { viewModel.onEvent(Event.QueryChanged(it)) },
                            onClear = { viewModel.onEvent(Event.ClearQuery) },
                            placeholder = "搜索主题标题、正文或回复",
                        )
                    },
                    secondary = {
                        ThreadFilterBar(
                            items = SearchType.entries,
                            selected = state.searchType,
                            label = SearchType::title,
                            onSelect = { viewModel.onEvent(Event.TypeChanged(it)) },
                        )
                    },
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (state.query.isBlank()) {
                    ModernEmptyState(
                            icon = Icons.Default.Search,
                            title = "寻找讨论与答案",
                            description = "输入关键词后，Thread 会在当前来源的主题和回复中搜索。",
                            modifier = Modifier.align(Alignment.Center),
                        )
                } else {
                    if (state.searchType == SearchType.THREAD) {
                        ThreadResultList(
                            viewModel = viewModel,
                            onThreadClick = { navigator.push(TopicDetailPage(it)) },
                            onImageClick = { threadId, img ->
                                navigator.push(
                                    ImagePreviewPage(
                                        ImagePreviewViewModelParams(initialImages = listOf(img)),
                                    )
                                )
                            },
                            onUserClick = { userHash -> navigator.push(UserDetailPage(sourceId, userHash)) }
                        )
                    } else {
                        ReplyResultList(
                            viewModel = viewModel,
                            onThreadClick = { navigator.push(TopicDetailPage(it)) },
                            onImageClick = { threadId, img ->
                                navigator.push(
                                    ImagePreviewPage(
                                        ImagePreviewViewModelParams(initialImages = listOf(img)),
                                    )
                                )
                            }
                        )
                    }
                }
                }
            }
        }
    }

    @Composable
    private fun ThreadResultList(
        viewModel: SearchViewModel,
        onThreadClick: (String) -> Unit,
        onImageClick: (String, Image) -> Unit,
        onUserClick: (String) -> Unit,
    ) {
        val threads =
            viewModel.state.collectAsStateWithLifecycle().value.threadPagingData.collectAsLazyPagingItems()

        PagingStateLayout(
            items = threads,
            modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            empty = {
                ModernEmptyState(
                    icon = Icons.Default.Search,
                    title = "没有找到主题",
                    description = "换一个关键词，或切换到回复搜索。",
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(threads.itemCount) { index ->
                    val thread = threads[index] ?: return@items
                    TopicCard(
                        topic = thread,
                        onClick = { onThreadClick(thread.id) },
                        onImageClick = { img -> onImageClick(thread.id, img) },
                        onUserClick = onUserClick,
                    )
                }
                item { PagingAppendState(threads) }
                }
            }
        }

    @Composable
    private fun ReplyResultList(
        viewModel: SearchViewModel,
        onThreadClick: (String) -> Unit,
        onImageClick: (String, Image) -> Unit,
    ) {
        val replies =
            viewModel.state.collectAsStateWithLifecycle().value.replyPagingData.collectAsLazyPagingItems()

        PagingStateLayout(
            items = replies,
            modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            empty = {
                ModernEmptyState(
                    icon = Icons.Default.Search,
                    title = "没有找到回复",
                    description = "尝试更短的关键词，或切换到主题搜索。",
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(replies.itemCount) { index ->
                    val reply = replies[index] ?: return@items
                    SearchReplyCard(
                        reply = reply,
                        onClick = { onThreadClick(reply.topicId) },
                        onImageClick = { img -> onImageClick(reply.topicId, img) },
                    )
                }
                item { PagingAppendState(replies) }
            }
        }
    }
}

@Composable
fun SearchReplyCard(
    reply: Comment,
    onClick: () -> Unit,
    onImageClick: (Image) -> Unit,
) {
    ThreadCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                Text(
                    text = reply.author.id,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No.${reply.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = ">> No.${reply.topicId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (reply.title.isNullOrBlank().not()) {
                Text(
                    text = reply.title!!,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            RichText(
                text = reply.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                blankLinePolicy = BlankLinePolicy.REMOVE
            )
        }
    }
}
