package ai.saniou.nmb.workflow.forum

import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.home.ListThreadPage
import ai.saniou.nmb.workflow.image.ImageInfo
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.image.ImagePreviewViewModelParams
import ai.saniou.nmb.workflow.post.PostPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
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
        val coroutineScope = rememberCoroutineScope()
        var expandedFab by remember { mutableStateOf(true) }
        val fabNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -5f) {
                        expandedFab = false
                    } else if (available.y > 5f) {
                        expandedFab = true
                    }
                    return Offset.Zero
                }
            }
        }

        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
            if (lazyListState.firstVisibleItemIndex == 0) {
                expandedFab = true
            }
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    ForumContract.Effect.ScrollToTop -> lazyListState.animateScrollToItem(0)
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(fabNestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.forumName,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                                            threads.refresh()
                                        } else {
                                            coroutineScope.launch {
                                                lazyListState.animateScrollToItem(0)
                                            }
                                        }
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
                    onClick = {
                        navigator.push(
                            PostPage(
                                fid = forumId.toInt(),
                                forumName = state.forumName
                            )
                        )
                    },
                    expanded = expandedFab,
                    icon = { Icon(Icons.Default.Add, "发帖") },
                    text = { Text("发帖") }
                )
            }
        ) { innerPadding ->
            ListThreadPage(
                state = lazyListState,
                threadFlow = viewModel.threads,
                onThreadClicked = { threadId -> navigator.push(ThreadPage(threadId)) },
                onImageClick = { _, imgPath, ext ->
                    val imageInfo = ImageInfo(imgPath, ext)
                    navigator.push(
                        ImagePreviewPage(
                            ImagePreviewViewModelParams(
                                initialImages = listOf(imageInfo),
                            )
                        )
                    )
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}





