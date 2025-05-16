package ai.saniou.nmb.workflow.home

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import cafe.adriel.voyager.core.screen.Screen
import org.jetbrains.compose.resources.stringResource

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No Snackbar Host State")
}

class HomeScreen : Screen {
    @Composable
    override fun Content() {

        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
        MaterialTheme {
            val snackbarHostState = remember { SnackbarHostState() }

            CompositionLocalProvider(
                values = arrayOf(
                    LocalSnackbarHostState provides snackbarHostState
                )
            ) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        AppDestinations.entries.forEach {
                            item(
                                icon = {
                                    Icon(
                                        it.icon,
                                        contentDescription = stringResource(it.contentDescription)
                                    )
                                },
                                label = { Text(stringResource(it.label)) },
                                selected = it == currentDestination,
                                onClick = { currentDestination = it }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) {
                        val a = rememberNavController()
                        when (currentDestination) {
                            AppDestinations.HOME -> HomePage()
                            AppDestinations.FAVORITES -> HomePage()

                            AppDestinations.SHOPPING -> ThreadDetailPane()
                            AppDestinations.PROFILE -> ThreadDetailPane()
                        }
                    }

                }
            }
        }

    }

}
