package ai.saniou.nmb.workflow.user

import ai.saniou.nmb.db.table.Cookie
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


data class UserPage(
    val di: DI = nmbdi,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userViewModel: UserViewModel = rememberScreenModel()
        val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
        var showAddCookieDialog by remember { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current

        Scaffold(
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
            Column(
                modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "在网页端登录后，请访问 https://www.nmbxd.com/Member/User/Index/home.html 获取饼干并在此处添加。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp).clickable {
                        uriHandler.openUri("https://www.nmbxd.com/Member/User/Index/home.html")
                    }
                )
                when (val state = uiState) {
                    is UserUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UserUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("错误: ${state.message}")
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { userViewModel.loadCookies() }) {
                                    Text("重试")
                                }
                            }
                        }
                    }

                    is UserUiState.Success -> {
                        CookieListContent(
                            cookies = state.cookies,
                            onDelete = { userViewModel.deleteCookie(it) },
                            onSortFinished = { newList -> userViewModel.updateCookieOrder(newList) }
                        )
                    }
                }
            }
        }

        if (showAddCookieDialog) {
            AddCookieDialog(
                onDismiss = { showAddCookieDialog = false },
                onConfirm = { name, value ->
                    userViewModel.addCookie(name, value)
                    showAddCookieDialog = false
                }
            )
        }
    }
}

@Composable
fun CookieListContent(
    cookies: List<Cookie>,
    onDelete: (Cookie) -> Unit,
    onSortFinished: (List<Cookie>) -> Unit,
) {
    var localCookies by remember(cookies) { mutableStateOf(cookies) }
    val lazyListState = rememberLazyListState()

    val state = rememberReorderableLazyListState(lazyListState) { from, to ->
        localCookies = localCookies.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onSortFinished(localCookies)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(localCookies, key = { _, it -> it.cookie }) { index, cookie ->
            ReorderableItem(state, key = cookie.cookie) {
                val interactionSource = remember { MutableInteractionSource() }

                Card(
                    onClick = {},
                    modifier = Modifier
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(
                                    label = "Move Up",
                                    action = {
                                        if (index > 0) {
                                            localCookies = localCookies.toMutableList().apply {
                                                add(index - 1, removeAt(index))
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                ),
                                CustomAccessibilityAction(
                                    label = "Move Down",
                                    action = {
                                        if (index < localCookies.size - 1) {
                                            localCookies = localCookies.toMutableList().apply {
                                                add(index + 1, removeAt(index))
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                ),
                            )
                        },
                    interactionSource = interactionSource,
                ) {
                    CookieItem(
                        cookie = cookie,
                        onDelete = { onDelete(cookie) },
                    )
                }
            }
        }
    }
}

@Composable
fun CookieItem(
    cookie: Cookie,
    onDelete: () -> Unit,
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(
        defaultElevation = 1.dp
    ),
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cookie.alias ?: "未命名",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "值: ${cookie.cookie}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "添加时间: ${cookie.createdAt}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除饼干")
            }
        }
    }
}

@Composable
fun AddCookieDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加饼干") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称 (可选)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("值") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(name, value) },
                    enabled = value.isNotBlank()
                ) {
                    Text("确认")
                }
            }
        }
    )
}
