package ai.saniou.forum.workflow.user

import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.coreui.composition.LocalForumSourceId
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.workflow.login.TiebaLoginScreen
import ai.saniou.forum.workflow.user.UserContract.Event
import ai.saniou.thread.data.source.tieba.TiebaMapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
        val sourceId = LocalForumSourceId.current
        val userViewModel: UserViewModel = rememberScreenModel()
        val state by userViewModel.state.collectAsStateWithLifecycle()
        var showAddCookieDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        val listState = rememberLazyListState()
        val isFabVisible by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex == 0 || listState.isScrollInProgress.not()
            }
        }

        LaunchedEffect(Unit) {
            userViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is UserContract.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                SaniouTopAppBar(
                    title = "用户中心",
                    onNavigationClick = { navigator.pop() },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                AnimatedVisibility(visible = isFabVisible) {
                    // Temporarily using different actions for debug/dev
                    // Long term this should be a proper menu or check source type
                    FloatingActionButton(onClick = {
                        if (sourceId == TiebaMapper.SOURCE_ID) {
                            navigator.push(TiebaLoginScreen())
                        } else {
                            showAddCookieDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "登录/添加账号")
                    }
                }
            }
        ) { paddingValues ->
            UserScreenContent(
                modifier = Modifier.padding(paddingValues),
                state = state,
                onEvent = userViewModel::handleEvent,
                scrollBehavior = scrollBehavior,
                listState = listState,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserScreenContent(
    state: UserContract.State,
    onEvent: (Event) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
    ) {
        // This content should not scroll
        UserGuideCard(
            onOpenUri = { uriHandler.openUri("https://www.nmbxd.com/Member/User/Index/home.html") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                    },
                    listState = listState,
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                )
            }
        }
}
}
