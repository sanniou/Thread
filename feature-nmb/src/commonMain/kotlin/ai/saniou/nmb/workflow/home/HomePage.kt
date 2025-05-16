package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.ui.components.HtmlTitleText
import ai.saniou.nmb.workflow.post.PostPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.nmb.workflow.user.UserPage
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.back_button


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaniouAppBar(
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
                Text(customTitle ?: "X")
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

@Composable
fun HomePage() {
    val navController = LocalNavigator.currentOrThrow

    // 控制抽屉状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 检查抽屉是否打开
    val isDrawerOpen = drawerState.isOpen

    Scaffold(
        topBar = {
            SaniouAppBar(
                canNavigateBack = navController.canPop,
                navigateUp = { navController.pop() },
                onUserIconClick = { navController.push(UserPage()) },
                onMenuClick = {
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        } else {
                            drawerState.open()
                        }
                    }
                },
                isDrawerOpen = isDrawerOpen,
            )
        }
    ) { innerPadding ->
        ForumCategoryPage(
            onThreadClicked = {
                navController.push(
                    ThreadPage(it)
                )
            },
            onNewPostClicked = { fid ->
                navController.push(
                    PostPage(fid)
                )
            },
            drawerState = drawerState,
        )
    }
}
