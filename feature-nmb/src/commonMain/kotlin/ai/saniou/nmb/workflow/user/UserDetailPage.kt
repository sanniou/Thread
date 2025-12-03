package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.ForumThreadCard
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.ThreadListSkeleton
import ai.saniou.nmb.workflow.image.ImageInfo
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.image.ImagePreviewViewModelParams
import ai.saniou.nmb.workflow.image.ThreadImageProvider
import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.nmb.workflow.thread.ThreadReply
import ai.saniou.nmb.workflow.thread.ThreadViewModel
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import cafe.adriel.voyager.kodein.rememberScreenModel
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
                    TopAppBar(
                        title = { Text(text = "用户: $userHash") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.handleEvent(UserDetailContract.Event.Back) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
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
                                            UserDetailContract.Tab.Threads -> "串"
                                            UserDetailContract.Tab.Replies -> "回复"
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
                    UserDetailContract.Tab.Threads -> {
                        state.threads?.let { flow ->
                            val threads = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(threads.itemCount) { index ->
                                    val thread = threads[index]
                                    if (thread != null) {
                                        ForumThreadCard(
                                            thread = thread,
                                            onClick = { navigator.push(ThreadPage(threadId = thread.id)) },
                                            onImageClick = { img, ext ->
                                                navigator.push(
                                                    ImagePreviewPage(
                                                        ImagePreviewViewModelParams(
                                                            initialIndex = 0,
                                                            initialImages = listOf(ImageInfo(img, ext)),
                                                            imageProvider = ThreadImageProvider(
                                                                thread.id,
                                                                nmbdi.direct.instance()
                                                            )
                                                        )
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }

                                when (threads.loadState.refresh) {
                                    is LoadStateLoading -> item { ThreadListSkeleton() }
                                    is LoadStateError -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(onClick = { threads.retry() }) {
                                                Text("重试")
                                            }
                                        }
                                    }
                                    else -> {}
                                }

                                when (threads.loadState.append) {
                                    is LoadStateLoading -> item { LoadingIndicator() }
                                    is LoadStateError -> item { LoadingFailedIndicator() }
                                    else -> {
                                        if (threads.loadState.append.endOfPaginationReached && threads.itemCount > 0) {
                                            item { LoadEndIndicator() }
                                        }
                                    }
                                }

                                if (threads.loadState.refresh !is LoadStateLoading && threads.itemCount == 0) {
                                     item {
                                        EmptyContent(message = "该用户还没有发布过串")
                                     }
                                }
                            }
                        }
                    }

                    UserDetailContract.Tab.Replies -> {
                        state.replies?.let { flow ->
                            val replies = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(replies.itemCount) { index ->
                                    val reply = replies[index]
                                    if (reply != null) {
                                        ThreadReply(
                                            reply = reply,
                                            poUserHash = "",
                                            onReplyClicked = { navigator.push(ThreadPage(threadId = reply.threadId)) },
                                            refClick = { navigator.push(ThreadPage(threadId = reply.threadId)) }, // 简化处理，暂时跳转到主串
                                            onImageClick = { img, ext ->
                                                 navigator.push(
                                                    ImagePreviewPage(
                                                        ImagePreviewViewModelParams(
                                                            initialIndex = 0,
                                                            initialImages = listOf(ImageInfo(img, ext)),
                                                            imageProvider = ThreadImageProvider(
                                                                reply.threadId,
                                                                nmbdi.direct.instance()
                                                            )
                                                        )
                                                    )
                                                )
                                            },
                                            onCopy = {},
                                            onBookmark = {},
                                            onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) },
                                        )
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
