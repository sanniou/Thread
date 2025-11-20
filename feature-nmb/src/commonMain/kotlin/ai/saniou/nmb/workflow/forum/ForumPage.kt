package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.ThreadCard
import ai.saniou.nmb.ui.components.ThreadListSkeleton
import ai.saniou.nmb.workflow.image.ImageInfo
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.image.ImagePreviewUiState
import ai.saniou.nmb.workflow.thread.ThreadPage
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.LoadState
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.LoadStateNotLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

data class ForumPage(
    val di: DI = nmbdi,
    val forumId: Long,
    val fgroupId: Long,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ForumViewModel = rememberScreenModel(tag = "${fgroupId}_${forumId}") {
            di.direct.instance(arg = forumId to fgroupId)
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val threads = viewModel.threads.collectAsLazyPagingItems()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        val lazyListState = rememberLazyListState()
        val expandedFab by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == 0 } }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    ForumContract.Effect.ScrollToTop -> lazyListState.animateScrollToItem(0)
                }
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.forumName,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        viewModel.onEvent(ForumContract.Event.ScrollToTop)
                                    }
                                )
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { /* TODO: Navigate to post screen */ },
                    expanded = expandedFab,
                    icon = { Icon(Icons.Default.Add, "发帖") },
                    text = { Text("发帖") }
                )
            }
        ) { innerPadding ->
            PullToRefreshWrapper(
                onRefreshTrigger = { threads.refresh() },
                modifier = Modifier.padding(innerPadding)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (threads.loadState.refresh) {
                        is LoadStateError -> ThreadListSkeleton()
                        is LoadStateError -> {
                            Button(
                                onClick = { threads.retry() },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text("加载失败，点击重试")
                            }
                        }

                        else -> {
                            if (threads.itemCount == 0) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Text("什么都没有", modifier = Modifier.align(Alignment.Center))
                                }
                            } else {
                                LazyColumn(
                                    state = lazyListState,
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(threads.itemCount, threads.itemKey { it.id }) { index ->
                                        threads[index]?.let { thread ->
                                            ThreadCard(
                                                thread = thread,
                                                onClick = { navigator.push(ThreadPage(thread.id)) },
                                                onImageClick = { imgPath, ext ->
                                                    val imageInfo = ImageInfo(imgPath, ext)
                                                    val uiState = ImagePreviewUiState(
                                                        images = listOf(imageInfo),
                                                        initialIndex = 0,
                                                        endReached = true
                                                    )
                                                    navigator.push(
                                                        ImagePreviewPage(
                                                            uiState = uiState,
                                                            di = di,
                                                            onLoadMore = {}
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    when (threads.loadState.append) {
                                        is LoadStateLoading -> item { LoadingIndicator() }
                                        is LoadStateError -> item { LoadingFailedIndicator() }
                                        is LoadStateNotLoading -> item { LoadEndIndicator() }
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





