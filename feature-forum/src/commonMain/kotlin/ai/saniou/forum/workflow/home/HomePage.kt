package ai.saniou.forum.workflow.home

import ai.saniou.forum.ui.components.AppBarTitle
import ai.saniou.forum.workflow.user.UserPage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.back_button

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { AppBarTitle(title = "") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    if (navController.canPop) {
                        IconButton(onClick = { navController.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back_button)
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isOpen) {
                                    drawerState.close()
                                } else {
                                    drawerState.open()
                                }
                            }
                        }) {
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
                    IconButton(onClick = { navController.push(ForumListPage) }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "论坛列表"
                        )
                    }
                    IconButton(onClick = { navController.push(UserPage()) }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "用户中心"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ForumCategoryPage(
                drawerState = drawerState,
            ).Content()
        }
    }
}
