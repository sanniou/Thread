package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.NmbScreen
import ai.saniou.nmb.workflow.forum.ForumScreen
import ai.saniou.nmb.workflow.image.ImagePreviewNavigationDestination
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.image.ImagePreviewViewModel
import ai.saniou.nmb.workflow.post.PostPage
import ai.saniou.nmb.ui.components.HtmlTitleText
import ai.saniou.nmb.workflow.subscription.SubscriptionNavigationDestination
import ai.saniou.nmb.workflow.subscription.SubscriptionPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.nmb.workflow.thread.ThreadPageNavigationDestination
import ai.saniou.nmb.workflow.user.UserPage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.back_button
import ai.saniou.nmb.di.nmbdi
import cafe.adriel.voyager.core.screen.Screen
import org.kodein.di.instance


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaniouAppBar(
    currentScreen: NmbScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    onUserIconClick: () -> Unit,
    onMenuClick: () -> Unit,
    showMenuIcon: Boolean = true,
    isDrawerOpen: Boolean = false,
    customTitle: String? = null,
    modifier: Modifier = Modifier,
    extraActions: @Composable (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            if (customTitle != null && (customTitle.contains("<b>") || customTitle.contains("<br>") || customTitle.contains(
                    "<small>"
                ))
            ) {
                // 如果标题包含HTML标签，则使用HtmlTitleText组件
                HtmlTitleText(text = customTitle)
            } else {
                // 否则使用普通Text
                Text(customTitle ?: currentScreen.name)
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_button)
                    )
                }
            } else if (showMenuIcon) {
                IconButton(onClick = onMenuClick) {
                    // 根据抽屉状态显示不同图标
                    Icon(
                        imageVector = if (isDrawerOpen)
                            Icons.AutoMirrored.Filled.ArrowBack
                        else
                            Icons.Default.Menu,
                        contentDescription = if (isDrawerOpen) "关闭菜单" else "打开菜单"
                    )
                }
            }
        },
        actions = {
            // 显示额外的操作按钮（如果有）
            extraActions?.invoke()

            // 用户图标按钮
            IconButton(onClick = onUserIconClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "用户中心"
                )
            }
        }
    )
}

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        HomePage()
        // ...
    }

}

@Composable
fun HomePage(
    navController: NavHostController = rememberNavController(),
    di: org.kodein.di.DI = nmbdi
) {
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = NmbScreen.valueOf(
        backStackEntry?.destination?.route?.split("/")?.get(0) ?: NmbScreen.ForumCategory.name
    )

    // 控制抽屉状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 检查抽屉是否打开
    val isDrawerOpen = drawerState.isOpen

    // 自定义标题状态
    val customTitle = remember { mutableStateOf<String?>(null) }

    // 额外的操作按钮状态
    val extraActions = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    // 监听导航变化，重置自定义标题和操作按钮
    LaunchedEffect(backStackEntry) {
        // 当导航到新页面时，重置自定义标题和操作按钮
        customTitle.value = null
        extraActions.value = null
    }

    Scaffold(
        topBar = {
            SaniouAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                onUserIconClick = { navController.navigate(NmbScreen.User.name) },
                onMenuClick = {
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        } else {
                            drawerState.open()
                        }
                    }
                },
                // 只在论坛分类页面显示菜单图标
                showMenuIcon = currentScreen == NmbScreen.ForumCategory,
                isDrawerOpen = isDrawerOpen,
                customTitle = customTitle.value,
                extraActions = extraActions.value
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NmbScreen.ForumCategory.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(route = NmbScreen.ForumCategory.name) {
                ForumCategoryPage(
                    onThreadClicked = { navController.navigate("${ThreadPageNavigationDestination.route}/${it}") },
                    onNewPostClicked = { fid ->
                        navController.navigate("${NmbScreen.Post.name}/${fid}")
                    },
                    onUpdateTitle = { title ->
                        customTitle.value = title
                    },
                    drawerState = drawerState,
                    navController = navController
                )
            }

            composable(route = "${NmbScreen.Forum.name}/{id}") {
                val forumId = it.arguments?.getString("id")?.toLongOrNull() ?: 0
                ForumScreen(
                    forumId = forumId,
                    onThreadClicked = { threadId ->
                        navController.navigate("${ThreadPageNavigationDestination.route}/${threadId}")
                    },
                    onNewPostClicked = { fid ->
                        navController.navigate("${NmbScreen.Post.name}/${fid}")
                    },
                    onUpdateTitle = { title ->
                    },
                    navController = navController
                )
            }

            composable(
                route = ThreadPageNavigationDestination.routeWithArg,
                arguments = listOf(navArgument(ThreadPageNavigationDestination.nameArg) {
                    type = NavType.LongType
                })
            ) {
                val threadId =
                    backStackEntry?.arguments?.getLong(ThreadPageNavigationDestination.nameArg)
                ThreadPage(
                    threadId = threadId,
                    onUpdateTitle = { title ->
                        customTitle.value = title
                    },
                    onSetupMenuButton = { menuButton ->
                        extraActions.value = menuButton
                    },
                    navController = navController
                )
            }

            composable(route = "${NmbScreen.Post.name}/{forumId}") {
                val forumId = it.arguments?.getString("forumId")?.toIntOrNull()
                PostPage(
                    forumId = forumId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable(route = "${NmbScreen.Post.name}/reply/{threadId}") {
                val threadId = it.arguments?.getString("threadId")?.toLongOrNull()
                PostPage(
                    threadId = threadId,
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            composable(route = NmbScreen.User.name) {
                UserPage(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            // 订阅列表页面
            composable(route = SubscriptionNavigationDestination.route) {
                SubscriptionPage(
                    onThreadClicked = { threadId ->
                        navController.navigate("${ThreadPageNavigationDestination.route}/${threadId}")
                    },
                    onUpdateTitle = { title ->
                        customTitle.value = title
                    },
                    navController = navController
                )
            }

            // 图片预览页面
            composable(
                route = ImagePreviewNavigationDestination.routeWithArgs,
                arguments = listOf(
                    navArgument(ImagePreviewNavigationDestination.imgPathArg) {
                        type = NavType.StringType
                        nullable = false
                    },
                    navArgument(ImagePreviewNavigationDestination.extArg) {
                        type = NavType.StringType
                        nullable = false
                    }
                )
            ) {
                // 获取并解码路径参数
                val encodedImgPath =
                    it.arguments?.getString(ImagePreviewNavigationDestination.imgPathArg) ?: ""
                val encodedExt =
                    it.arguments?.getString(ImagePreviewNavigationDestination.extArg) ?: ""

                // 使用 ImagePreviewNavigationDestination 的解码方法
                val imgPath = ImagePreviewNavigationDestination.decodePath(encodedImgPath)
                val ext = ImagePreviewNavigationDestination.decodePath(encodedExt)

                val imagePreviewViewModel: ImagePreviewViewModel = viewModel {
                    val viewModel by di.instance<ImagePreviewViewModel>()
                    viewModel
                }

                ImagePreviewPage(
                    imgPath = imgPath,
                    ext = ext,
                    onNavigateBack = { navController.navigateUp() },
                    onSaveImage = { path, extension ->
                        // 保存图片
                        imagePreviewViewModel.setCurrentImage(path, extension)
                        imagePreviewViewModel.saveCurrentImage()
                    },
                    onUpdateTitle = { title ->
                        customTitle.value = title
                    },
                    onSetupMenuButton = { menuButton ->
                        extraActions.value = menuButton
                    }
                )
            }
        }
    }
}

interface NavigationDestination {
    val route: String
}
