package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.DrawerHeader
import ai.saniou.nmb.ui.components.DrawerMenuItem
import ai.saniou.nmb.ui.components.DrawerMenuRow
import ai.saniou.nmb.workflow.forum.ForumContent
import ai.saniou.nmb.workflow.image.ImagePreviewNavigationDestination
import ai.saniou.nmb.workflow.subscription.SubscriptionNavigationDestination
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance


@Composable
fun ForumCategoryPage(
    di: DI = nmbdi,
    onThreadClicked: (Long) -> Unit,
    onNewPostClicked: (Long) -> Unit,
    onUpdateTitle: ((String) -> Unit)?,
    drawerState: DrawerState,
    navController: NavController = rememberNavController()
) {
    val forumCategoryViewModel: ForumCategoryViewModel = viewModel {
        val forumCategoryViewModel by di.instance<ForumCategoryViewModel>()
        forumCategoryViewModel;
    }
    val categoryContent by forumCategoryViewModel.uiState.collectAsStateWithLifecycle()

    val greetImageViewModel: GreetImageViewModel = viewModel {
        val greetImageViewModel by di.instance<GreetImageViewModel>()
        greetImageViewModel
    }
    val greetImageUrl by greetImageViewModel.greetImageUrl.collectAsStateWithLifecycle()
    val isGreetImageLoading by greetImageViewModel.isLoading.collectAsStateWithLifecycle()

    // 创建协程作用域用于控制抽屉
    val scope = rememberCoroutineScope()

    // 监听当前选中的 forum 并自动加载
    LaunchedEffect(categoryContent.currentForum) {
        categoryContent.currentForum?.let { forumId ->
            onUpdateTitle?.invoke(categoryContent.currentForum ?: "")
        }
    }

    ForumCategoryUi(
        uiState = categoryContent,
        onThreadClicked = onThreadClicked,
        onNewPostClicked = onNewPostClicked,
        onUpdateTitle = onUpdateTitle,
        drawerState = drawerState,
        scope = scope,
        navController = navController,
        greetImageUrl = greetImageUrl,
        isGreetImageLoading = isGreetImageLoading
    )
}

@Composable
@Preview
fun ForumCategoryUi(
    uiState: ForumCategoryUiState,
    onThreadClicked: (Long) -> Unit,
    onNewPostClicked: (Long) -> Unit,
    onUpdateTitle: ((String) -> Unit)?,
    drawerState: DrawerState,
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavController = rememberNavController(),
    greetImageUrl: String? = null,
    isGreetImageLoading: Boolean = false
) {
    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    // 使用Box将DrawerHeader作为背景层，其他内容可以叠加在上面
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 欢迎图片作为背景，高度为drawer的1/3
                        DrawerHeader(imageUrl = greetImageUrl)

                        // 内容区域，包含菜单和论坛列表
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .padding(top = 140.dp)
                                .background(Color.White.copy(alpha = 0.8f))
                        ) {
                            // 菜单入口行：订阅列表、访问历史、发言记录、搜索按钮
                            DrawerMenuRow(
                                menuItems = listOf(
                                    DrawerMenuItem(
                                        icon = Icons.Default.Favorite,
                                        label = "订阅列表",
                                        onClick = {
                                            // 导航到订阅列表页面
                                            navController.navigate(SubscriptionNavigationDestination.route)
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        }
                                    ),
                                    DrawerMenuItem(
                                        icon = Icons.Default.Home,
                                        label = "访问历史",
                                        onClick = {
                                            // TODO: 导航到访问历史页面
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        }
                                    ),
                                    DrawerMenuItem(
                                        icon = Icons.Default.Send,
                                        label = "发言记录",
                                        onClick = {
                                            // TODO: 导航到发言记录页面
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        }
                                    ),
                                    DrawerMenuItem(
                                        icon = Icons.Default.Search,
                                        label = "搜索",
                                        onClick = {
                                            // TODO: 导航到搜索页面
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        }
                                    )
                                ),
                                modifier = Modifier
                            )

                            // 分隔线
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                                // 使用LazyColumn显示论坛列表，并确保它可以滚动
                                LazyColumn(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(uiState.forums) { category ->
                                        // 分类项
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    uiState.onCategoryClick(category.id)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 分类图标
                                            Icon(
                                                imageVector = if (uiState.expandCategory == category.id)
                                                    Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                                                                // 选择论坛
                                                                uiState.onForumClick(forum.id)
                                                                // 关闭抽屉
                                                                scope.launch {
                                                                    drawerState.close()
                                                                }
                                                            }
                                                            .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                            )
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
                }
            }
        ) {
            // 内容区域 - 使用可复用的ForumContent组件
            uiState.currentForum?.let { forumId ->
                val id = forumId.toLong()
                ForumContent(
                    forumId = id,
                    onThreadClicked = onThreadClicked,
                    onNewPostClicked = onNewPostClicked,
                    onUpdateTitle = onUpdateTitle,
                    showFloatingActionButton = true,
                    onImageClick = { imgPath, ext ->
                        // 导航到图片预览页面
                        navController.navigate(
                            ImagePreviewNavigationDestination.createRoute(
                                imgPath,
                                ext
                            )
                        )
                    }
                )
            } ?: run {
                // 未选择论坛时显示提示
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("请从左侧选择一个论坛")
                }
            }
        }
    }
}

