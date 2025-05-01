package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.SkeletonLoader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * 可复用的论坛内容组件
 *
 * 该组件负责显示论坛内容，包括加载状态、错误状态和成功状态
 * 可以在多个地方使用，如 ForumScreen 和 ForumCategoryPage
 */
@Composable
fun ForumContent(
    forumId: Long,
    onThreadClicked: (Long) -> Unit,
    onNewPostClicked: (Long) -> Unit = {},
    onUpdateTitle: ((String) -> Unit)? = null,
    showFloatingActionButton: Boolean = true,
    onImageClick: ((String, String) -> Unit)? = null,
    di: DI = nmbdi
) {
    val forumViewModel: ForumViewModel = viewModel {
        val forumViewModel by di.instance<ForumViewModel>()
        forumViewModel
    }

    // 使用LaunchedEffect设置forumId，确保只在forumId变化时触发
    LaunchedEffect(forumId) {
        forumViewModel.setForumId(forumId)
    }

    val forumContent by forumViewModel.uiState.collectAsStateWithLifecycle()

    // 当论坛数据加载成功后，更新标题
    LaunchedEffect(forumContent) {
        if (forumContent is UiStateWrapper.Success<*>) {
            val state = (forumContent as UiStateWrapper.Success<ShowForumUiState>).value
            if (state != null && state.forumName.isNotBlank()) {
                onUpdateTitle?.invoke(state.forumName)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        forumContent.LoadingWrapper<ShowForumUiState>(
            content = {
                Forum(it, onThreadClicked, onImageClick = onImageClick)
            },
            error = {
                // 错误状态显示
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { forumViewModel.refreshForum() }
                        ) {
                            Text("重试")
                        }
                    }
                }
            },
            loading = {
                // 加载状态显示骨架屏
                SkeletonLoader()
            },
            onRetryClick = {
                // 重试时刷新当前论坛
                forumViewModel.refreshForum()
            }
        )

        // 添加发帖按钮
        if (showFloatingActionButton && forumContent is UiStateWrapper.Success<*>) {
            val state = (forumContent as UiStateWrapper.Success<ShowForumUiState>).value!!
            if (state.id > 0) {
                FloatingActionButton(
                    onClick = { onNewPostClicked(state.id) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "发布新帖"
                    )
                }
            }
        }
    }
}
