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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
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
        val coroutineScope = rememberCoroutineScope()
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            scrollState.scrollBy(-delta)
                        }
                    },
                ),
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
                        EmptyForumCard { forumList.refresh() }
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

@Composable
fun EmptyForumCard(
    emptyClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "暂无帖子",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "该板块当前没有帖子，点击右下角按钮发布第一个帖子吧！",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = emptyClick
            ) {
                Text("刷新")
            }
        }
    }
}




