package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.RefreshCard
import ai.saniou.nmb.ui.components.SkeletonLoader
import ai.saniou.nmb.ui.components.ThreadCard
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI


/**
 * ForumScreen 作为一个轻量级包装器，用于独立页面导航
 *
 * 该组件主要用于从非Drawer入口（如搜索结果、收藏列表等）进入论坛
 */

data class ForumScreen(
    val di: DI = nmbdi,
    val onThreadClicked: (Long) -> Unit = {},
    val onNewPostClicked: (Long) -> Unit = {},
    val forumId: Long,
    val fgroupId: Long,
) : Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            // 使用可复用的ForumContent组件
            ForumContent(
                forumId = forumId,
                fgroupId = fgroupId,
                onThreadClicked = onThreadClicked,
                onNewPostClicked = onNewPostClicked,
                showFloatingActionButton = true,
                onImageClick = { imgPath, ext ->
                    // 导航到图片预览页面
                    navigator.push(ImagePreviewPage(imgPath, ext))
                }
            )
        }
    }
}

@Composable
fun Forum(
    uiState: ShowForumUiState,
    onThreadClicked: (Long) -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    innerPadding: PaddingValues? = null
) {
    val forumList = uiState.forum.collectAsLazyPagingItems()
    PullToRefreshWrapper(
        onRefreshTrigger = {
            forumList.refresh()
        },
        modifier = Modifier.run {
            if (innerPadding != null) {
                padding(innerPadding)
            } else {
                this
            }
        }
    ) {
        val scrollState = rememberLazyListState()
        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(forumList.itemCount, forumList.itemKey { it.id }) { index ->
                forumList[index]?.let { thread ->
                    ThreadCard(
                        thread = thread,
                        onClick = { onThreadClicked(thread.id) },
                        onImageClick = onImageClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            when (forumList.loadState.refresh) {
                is LoadState.Error -> {
                    item {
                        RefreshCard(forumList)
                    }
                }

                is LoadState.Loading -> {
                    item {
                        SkeletonLoader()
                    }
                }

                is LoadState.NotLoading -> {
                    item {
                        if (forumList.itemCount == 0) {
                            LoadEndIndicator()
                        }
                    }
                }
            }

            when (forumList.loadState.append) {
                is LoadState.Loading -> {
                    item {
                        LoadingIndicator()
                    }
                }

                is LoadState.Error -> {
                    item {
                        LoadingFailedIndicator()
                    }
                }

                is LoadState.NotLoading -> {
                    item {
                        LoadEndIndicator()
                    }

                }
            }
        }
    }
}





