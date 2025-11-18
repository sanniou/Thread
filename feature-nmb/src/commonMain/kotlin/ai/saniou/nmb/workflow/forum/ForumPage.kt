package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.RefreshCard
import ai.saniou.nmb.ui.components.ThreadCard
import ai.saniou.nmb.ui.components.ThreadListSkeleton
import ai.saniou.nmb.workflow.forum.ForumContract.Event
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI
import org.kodein.di.compose.viewmodel.rememberViewModel
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
        val viewModel: ForumViewModel by rememberViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(forumId, fgroupId) {
            viewModel.onEvent(Event.LoadForum(forumId, fgroupId))
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.forumName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { innerPadding ->
            when {
                state.isLoading -> ThreadListSkeleton()
                state.error != null -> {
                    Button(onClick = { viewModel.onEvent(Event.Refresh) }) {
                        Text("加载失败，点击重试")
                    }
                }

                else -> ForumContent(
                    state = state,
                    innerPadding = innerPadding,
                    onThreadClicked = { threadId -> navigator.push(ThreadPage(threadId)) },
                    onImageClick = { imgPath, ext ->
                        navigator.push(
                            ImagePreviewPage(
                                imgPath,
                                ext
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ForumContent(
    state: ForumContract.State,
    innerPadding: PaddingValues,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
) {
    val threads = state.threads.collectAsLazyPagingItems()

    PullToRefreshWrapper(
        onRefreshTrigger = { threads.refresh() },
        modifier = Modifier.padding(innerPadding)
    ) {
        val scrollState = rememberLazyListState()
        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(threads.itemCount, threads.itemKey {
                it.id
            }) { index ->
                threads[index]?.let { thread ->
                    ThreadCard(
                        thread = thread,
                        onClick = { onThreadClicked(thread.id) },
                        onImageClick = onImageClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            when (threads.loadState.refresh) {
                is LoadState.Error -> item { RefreshCard(threads) }
                is LoadState.Loading -> item { ThreadListSkeleton() }
                is LoadState.NotLoading -> {
                    if (threads.itemCount == 0) {
                        item { LoadEndIndicator() }
                    }
                }
            }

            when (threads.loadState.append) {
                is LoadState.Loading -> item { LoadingIndicator() }
                is LoadState.Error -> item { LoadingFailedIndicator() }
                is LoadState.NotLoading -> item { LoadEndIndicator() }
            }
        }
    }
}





