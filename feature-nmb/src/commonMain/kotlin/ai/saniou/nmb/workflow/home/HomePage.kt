package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.NmbScreen
import ai.saniou.nmb.workflow.forum.ForumScreen
import ai.saniou.nmb.workflow.post.PostPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.nmb.workflow.thread.ThreadPageNavigationDestination
import ai.saniou.nmb.workflow.user.UserPage
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.jetbrains.compose.resources.stringResource
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.back_button


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaniouAppBar(
    currentScreen: NmbScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    onUserIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(currentScreen.name) },
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
            }
        },
        actions = {
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
fun HomePage(navController: NavHostController = rememberNavController()) {
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = NmbScreen.valueOf(
        backStackEntry?.destination?.route?.split("/")?.get(0) ?: NmbScreen.ForumCategory.name
    )
    Scaffold(
        topBar = {
            SaniouAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                onUserIconClick = { navController.navigate(NmbScreen.User.name) }
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
                    onThreadClicked = { navController.navigate("${ThreadPageNavigationDestination.route}/${it}") }
                )
            }

            composable(route = "${NmbScreen.Forum.name}/{id}") {
                val forumId = it.arguments?.getString("id")?.toLongOrNull() ?: 0
                ForumScreen(
                    onThreadClicked = { threadId ->
                        navController.navigate("${ThreadPageNavigationDestination.route}/${threadId}")
                    },
                    onNewPostClicked = { fid ->
                        navController.navigate("${NmbScreen.Post.name}/${fid}")
                    }
                )
            }

            composable(
                route = ThreadPageNavigationDestination.routeWithArg,
                arguments = listOf(navArgument(ThreadPageNavigationDestination.nameArg) {
                    type = NavType.LongType
                })
            ) {
                val threadId = backStackEntry?.arguments?.getLong(ThreadPageNavigationDestination.nameArg)
                ThreadPage(threadId)
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
        }
    }
}

interface NavigationDestination {
    val route: String
}
