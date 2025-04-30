package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.forum.Forum
import ai.saniou.nmb.workflow.forum.ForumViewModel
import ai.saniou.nmb.workflow.forum.ShowForumUiState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance


@Composable
fun ForumCategoryPage(
    di: DI = nmbdi,
    onThreadClicked: (Long) -> Unit,
    drawerState: DrawerState
) {
    val forumCategoryViewModel: ForumCategoryViewModel = viewModel {
        val forumCategoryViewModel by di.instance<ForumCategoryViewModel>()
        forumCategoryViewModel;
    }
    val content by forumCategoryViewModel.uiState.collectAsStateWithLifecycle()

    val forumViewModel: ForumViewModel = viewModel {
        val forumViewModel by di.instance<ForumViewModel>()
        forumViewModel;
    }
    val forumContent by forumViewModel.uiState.collectAsStateWithLifecycle()

    // 监听当前选中的 forum 并自动加载
    LaunchedEffect(content.currentForum) {
        content.currentForum?.let { forumId ->
            try {
                val id = forumId.toLong()
                forumViewModel.setForumId(id)
            } catch (e: NumberFormatException) {
                // 处理转换异常
            }
        }
    }

    ForumCategoryUi(
        uiState = content,
        forumContent = forumContent,
        forumViewModel = forumViewModel,
        onThreadClicked = onThreadClicked,
        drawerState = drawerState
    )
}

@Composable
@Preview
fun ForumCategoryUi(
    uiState: ForumCategoryUiState,
    forumContent: UiStateWrapper,
    forumViewModel: ForumViewModel,
    onThreadClicked: (Long) -> Unit,
    drawerState: DrawerState
) {
    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    // 添加抽屉标题
                    Text(
                        text = "论坛分类",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 如果论坛列表为空，显示加载提示
                    if (uiState.forums.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        LazyColumn {
                            items(uiState.forums) { category ->
                                // 分类项
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            uiState.onCategoryClick(category.id)
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 分类图标
                                    Icon(
                                        imageVector = if (uiState.expandCategory == category.id)
                                            Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = if (uiState.expandCategory == category.id)
                                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 分类名称
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (uiState.expandCategory == category.id)
                                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // 子论坛列表
                                AnimatedVisibility(uiState.expandCategory == category.id) {
                                    Column {
                                        category.forums.forEach { forum ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        uiState.onForumClick(forum.id)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                                    .padding(start = 32.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 论坛选中指示器
                                                if (uiState.currentForum == forum.id) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary,
                                                                CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                // 论坛名称
                                                Text(
                                                    text = forum.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (uiState.currentForum == forum.id)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            // 内容区域
            forumContent.LoadingWrapper<ShowForumUiState>(
                content = {
                    Forum(it, onThreadClicked)
                },
                onRetryClick = {
                    // 重试逻辑
                    uiState.currentForum?.let { forumId ->
                        try {
                            val id = forumId.toLong()
                            forumViewModel.setForumId(id)
                        } catch (e: NumberFormatException) {
                            // 处理转换异常
                        }
                    }
                }
            )
        }
    }
}

