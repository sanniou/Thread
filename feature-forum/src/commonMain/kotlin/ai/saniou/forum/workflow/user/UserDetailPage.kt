package ai.saniou.forum.workflow.user

import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.instance

data class UserDetailPage(
    val userHash: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: UserDetailViewModel = rememberScreenModel(tag = userHash) {
            nmbdi.direct.instance(arg = userHash)
        }

        val state by viewModel.state.collectAsState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
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

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    SaniouTopAppBar(
                        title = "用户: $userHash",
                        onNavigationClick = { viewModel.handleEvent(UserDetailContract.Event.Back) },
                        scrollBehavior = scrollBehavior
                    )
                    SecondaryTabRow(selectedTabIndex = state.currentTab.ordinal) {
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
                                            UserDetailContract.Tab.Topics -> "串"
                                            UserDetailContract.Tab.Comments -> "回复"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) { page ->
                when (UserDetailContract.Tab.entries[page]) {
                    UserDetailContract.Tab.Topics -> {
                        state.topics?.let { flow ->
                            val topics = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
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
                                            }
                                        )
                                    }
                                }

                                when (topics.loadState.refresh) {
                                    is LoadStateLoading -> item { ThreadListSkeleton() }
                                    is LoadStateError -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(onClick = { topics.retry() }) {
                                                Text("重试")
                                            }
                                        }
                                    }

                                    else -> {}
                                }

                                when (topics.loadState.append) {
                                    is LoadStateLoading -> item { LoadingIndicator() }
                                    is LoadStateError -> item { LoadingFailedIndicator() }
                                    else -> {
                                        if (topics.loadState.append.endOfPaginationReached && topics.itemCount > 0) {
                                            item { LoadEndIndicator() }
                                        }
                                    }
                                }

                                if (topics.loadState.refresh !is LoadStateLoading && topics.itemCount == 0) {
                                    item {
                                        EmptyContent(message = "该用户还没有发布过串")
                                    }
                                }
                            }
                        }
                    }

                    UserDetailContract.Tab.Comments -> {
                        state.comments?.let { flow ->
                            val replies = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(replies.itemCount) { index ->
                                    val reply = replies[index]
                                    if (reply != null) {
                                        Column {
                                            if (reply.title.isNullOrBlank().not() && reply.title != "无标题") {
                                                Text(
                                                    text = "回复串: ${reply.title}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "回复串: No.${reply.topicId}",
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
                                                onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) },
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
                                    is LoadStateLoading -> item { LoadingIndicator() }
                                    is LoadStateError -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(onClick = { replies.retry() }) {
                                                Text("重试")
                                            }
                                        }
                                    }

                                    else -> {}
                                }

                                when (replies.loadState.append) {
                                    is LoadStateLoading -> item { LoadingIndicator() }
                                    is LoadStateError -> item { LoadingFailedIndicator() }
                                    else -> {
                                        if (replies.loadState.append.endOfPaginationReached && replies.itemCount > 0) {
                                            item { LoadEndIndicator() }
                                        }
                                    }
                                }

                                if (replies.loadState.refresh !is LoadStateLoading && replies.itemCount == 0) {
                                    item {
                                        EmptyContent(message = "该用户还没有发布过回复")
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
