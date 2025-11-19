package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.user.UserContract.Event
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.DI


data class UserPage(
    val di: DI = nmbdi,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userViewModel: UserViewModel = rememberScreenModel()
        val state by userViewModel.state.collectAsStateWithLifecycle()
        var showAddCookieDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            userViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is UserContract.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("用户中心") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddCookieDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加饼干")
                }
            }
        ) { paddingValues ->
            UserScreenContent(
                modifier = Modifier.padding(paddingValues),
                state = state,
                onEvent = userViewModel::handleEvent
            )
        }

        if (showAddCookieDialog) {
            AddCookieDialog(
                onDismiss = { showAddCookieDialog = false },
                onConfirm = { name, value ->
                    userViewModel.handleEvent(Event.AddCookie(name, value))
                    showAddCookieDialog = false
                }
            )
        }
    }
}

@Composable
private fun UserScreenContent(
    state: UserContract.State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        UserGuideCard(
            onOpenUri = { uriHandler.openUri("https://www.nmbxd.com/Member/User/Index/home.html") },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("错误: ${state.error}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onEvent(Event.LoadCookies) }) {
                            Text("重试")
                        }
                    }
                }
            }

            else -> {
                CookieListContent(
                    cookies = state.cookies,
                    onDelete = { onEvent(Event.DeleteCookie(it)) },
                    onSortFinished = { newList ->
                        onEvent(
                            Event.UpdateCookieOrder(
                                newList
                            )
                        )
                    }
                )
            }
        }
    }
}
