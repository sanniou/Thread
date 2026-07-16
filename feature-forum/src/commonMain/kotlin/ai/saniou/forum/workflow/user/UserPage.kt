package ai.saniou.forum.workflow.user

import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.forum.ui.login.LoginScreen
import ai.saniou.forum.workflow.user.UserContract.Event
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
class UserPage : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sourceId = LocalForumSourceId.current
        val userViewModel: UserViewModel = rememberScreenModel()
        val state by userViewModel.state.collectAsStateWithLifecycle()
        var showLoginScreen by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val listState = rememberLazyListState()

        LaunchedEffect(sourceId) {
            userViewModel.handleEvent(Event.LoadData(sourceId))
        }

        LaunchedEffect(Unit) {
            userViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is UserContract.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        ThreadDetailScaffold(
            title = "用户中心",
            eyebrow = "IDENTITIES",
            subtitle = "管理当前来源的登录身份 · $sourceId",
            onBack = navigator::pop,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            actions = {
                if (state.loginStrategy != null) {
                    IconButton(onClick = { showLoginScreen = true }) {
                        Icon(Icons.Default.Add, contentDescription = "登录/添加账号")
                    }
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                UserScreenContent(
                    modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
                    state = state,
                    onEvent = userViewModel::handleEvent,
                    listState = listState,
                )
            }
        }

        if (showLoginScreen && state.loginStrategy != null) {
            LoginScreen(
                strategy = state.loginStrategy!!,
                onDismissRequest = { showLoginScreen = false },
                onLoginSuccess = { inputs ->
                    userViewModel.handleEvent(Event.AddAccount(inputs))
                    showLoginScreen = false
                }
            )
        }
    }
}

@Composable
private fun UserScreenContent(
    state: UserContract.State,
    onEvent: (Event) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
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
                        Button(onClick = { onEvent(Event.LoadData(state.sourceId)) }) {
                            Text("重试")
                        }
                    }
                }
            }

            else -> {
                CookieListContent(
                    cookies = state.cookies,
                    onDelete = { onEvent(Event.DeleteAccount(it)) },
                    onSortFinished = { newList ->
                        onEvent(
                            Event.UpdateAccountOrder(
                                newList
                            )
                        )
                    },
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
