package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.NmbScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
fun CupcakeAppBar(
    currentScreen: NmbScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
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
            CupcakeAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NmbScreen.ForumCategory.name,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            composable(route = NmbScreen.ForumCategory.name) {
                ForumCategoryPage(
                    onThreadClicked = { navController.navigate("${ThreadPageNavigationDestination.route}/${it}") }
                )
            }

            composable(route = "${NmbScreen.Forum.name}/{id}") {
                ForumScreen(
                    onThreadClicked = { navController.navigate("${ThreadPageNavigationDestination.route}/${it}") }
                )
            }

            composable(
                route = ThreadPageNavigationDestination.routeWithArg,
                arguments = listOf(navArgument(ThreadPageNavigationDestination.nameArg) {
                    type = NavType.LongType
                })
            ) {
                ThreadPage(backStackEntry?.arguments?.getLong(ThreadPageNavigationDestination.nameArg))
            }

        }

    }
}

interface NavigationDestination {
    val route: String
}

object ThreadPageNavigationDestination : NavigationDestination {
    override val route = NmbScreen.Thread.name
    const val nameArg = "tid"
    val routeWithArg = "$route/{$nameArg}"
}

@Composable
fun ThreadPage(name: Long?) {
    Text(
        "A\nB\nC\nD\nE\nF\nG\nH\nI\nJ\nK\nL\nM\nN\nO\nP\nQ\nR\nS\nT\nU\nV\nW\nX\nY\nZ\n"
                + NmbScreen.Thread.name + " " + name
    )
}
