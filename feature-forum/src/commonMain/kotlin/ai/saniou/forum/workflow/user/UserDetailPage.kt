package ai.saniou.forum.workflow.user

import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.topicdetail.ThreadReply
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.compose.collectAsLazyPagingItems
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.coreui.widgets.ThreadLoadingState
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.compose.localDI
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.eyebrow_member_activity
import thread.feature_forum.generated.resources.empty_title
import thread.feature_forum.generated.resources.post_page_reply
import thread.feature_forum.generated.resources.retry
import thread.feature_forum.generated.resources.s_1f63c99409
import thread.feature_forum.generated.resources.s_2d0ce11e3d
import thread.feature_forum.generated.resources.s_76f1ed24cb
import thread.feature_forum.generated.resources.s_ad941b51d3
import thread.feature_forum.generated.resources.s_bdf32f0d53
import thread.feature_forum.generated.resources.s_dca79914e5

data class UserDetailPage(
    val sourceId: String,
    val userHash: String,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val di = localDI()
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: UserDetailViewModel = rememberScreenModel(tag = "$sourceId:$userHash") {
            di.direct.instance(arg = sourceId to userHash)
        }

        val state by viewModel.state.collectAsState()
        val pagerState = rememberPagerState(pageCount = { UserDetailContract.Tab.entries.size })
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    UserDetailContract.Effect.NavigateBack -> navigator.pop()
                }
            }
        }

        LaunchedEffect(state.currentTab) {
            pagerState.animateScrollToPage(state.currentTab.ordinal)
        }

        LaunchedEffect(pagerState.currentPage) {
            viewModel.handleEvent(UserDetailContract.Event.SwitchTab(UserDetailContract.Tab.entries[pagerState.currentPage]))
        }

        ThreadDetailScaffold(
            title = userHash,
            eyebrow = stringResource(Res.string.eyebrow_member_activity),
            subtitle = stringResource(Res.string.s_dca79914e5),
            onBack = { viewModel.handleEvent(UserDetailContract.Event.Back) },
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues).fillMaxSize()) {
                SecondaryTabRow(
                    selectedTabIndex = state.currentTab.ordinal,
                    modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                        .align(Alignment.CenterHorizontally),
                ) {
                    UserDetailContract.Tab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = state.currentTab.ordinal == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = when (tab) {
                                        UserDetailContract.Tab.Topics -> stringResource(Res.string.s_1f63c99409)
                                        UserDetailContract.Tab.Comments -> stringResource(Res.string.post_page_reply)
                                    }
                                )
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth)
                        .align(Alignment.CenterHorizontally),
                ) { page ->
                    when (UserDetailContract.Tab.entries[page]) {
                    UserDetailContract.Tab.Topics -> {
                        state.topics?.let { flow ->
                            val topics = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                                    vertical = 12.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(topics.itemCount) { index ->
                                    val topic = topics[index]
                                    if (topic != null) {
                                        TopicCard(
                                            topic = topic,
                                            onClick = { navigator.push(TopicDetailPage(threadId = topic.id)) },
                                            onImageClick = { img ->
                                                navigator.push(
                                                    ImagePreviewPage(
                                                        ImagePreviewViewModelParams(
                                                            initialImages = listOf(img),
                                                        )
                                                    )
                                                )
                                            },
                                            modifier = threadAnimateItem(),
                                        )
                                    }
                                }

                                when (topics.loadState.refresh) {
                                    is Loading -> item { ThreadListSkeleton() }
                                    is Error -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SaniouButton(onClick = { topics.retry() }, text = stringResource(Res.string.retry))
                                        }
                                    }

                                    else -> {}
                                }

                                item { PagingAppendState(topics) }

                                if (topics.loadState.refresh !is Loading && topics.itemCount == 0) {
                                    item {
                                        EmptyContent(message = stringResource(Res.string.s_76f1ed24cb))
                                    }
                                }
                            }
                        }
                    }

                    UserDetailContract.Tab.Comments -> {
                        state.comments?.let { flow ->
                            val replies = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                                    vertical = 12.dp,
                                ),
                            ) {
                                items(replies.itemCount) { index ->
                                    val reply = replies[index]
                                    if (reply != null) {
                                        Column {
                                            val replyTitle = reply.title
                                            if (!replyTitle.isNullOrBlank() && replyTitle != stringResource(Res.string.empty_title)) {
                                                Text(
                                                    text = stringResource(Res.string.s_2d0ce11e3d, replyTitle),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(Res.string.s_bdf32f0d53, reply.topicId),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            }

                                            ThreadReply(
                                                reply = reply,
                                                poUserHash = "",
                                                onReplyClicked = { navigator.push(TopicDetailPage(threadId = reply.topicId)) },
                                                refClick = { navigator.push(TopicDetailPage(threadId = reply.topicId)) }, // 简化处理，暂时跳转到主串
                                                onImageClick = { img ->
                                                    navigator.push(
                                                        ImagePreviewPage(
                                                            ImagePreviewViewModelParams(
                                                                initialImages = listOf(img)),
                                                            )
                                                    )
                                                },
                                                onCopy = {},
                                                onBookmark = {},
                                                onUserClick = { userHash -> navigator.push(UserDetailPage(sourceId, userHash)) },
                                                onBookmarkImage = { _ -> }
                                            )
                                        }
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                when (replies.loadState.refresh) {
                                    is Loading -> item {
                                        ThreadLoadingState(modifier = Modifier.fillMaxWidth())
                                    }
                                    is Error -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SaniouButton(onClick = { replies.retry() }, text = stringResource(Res.string.retry))
                                        }
                                    }

                                    else -> {}
                                }

                                item { PagingAppendState(replies) }

                                if (replies.loadState.refresh !is Loading && replies.itemCount == 0) {
                                    item {
                                        EmptyContent(message = stringResource(Res.string.s_ad941b51d3))
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
    private fun EmptyContent(
        modifier: Modifier = Modifier,
        message: String,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
