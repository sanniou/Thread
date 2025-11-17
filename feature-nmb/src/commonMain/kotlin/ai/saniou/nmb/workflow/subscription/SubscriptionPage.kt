package ai.saniou.nmb.workflow.subscription

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.SkeletonLoader
import ai.saniou.nmb.ui.components.SubscriptionCard
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.subscription.SubscriptionContract.Event
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.DI
import org.kodein.di.instance

data class SubscriptionPage(
    val di: DI = nmbdi,
    val onUpdateTitle: ((String) -> Unit)? = null,
    val onThreadClicked: (Long) -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        val viewModel: SubscriptionViewModel = viewModel {
            val viewModel by di.instance<SubscriptionViewModel>()
            viewModel
        }
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            onUpdateTitle?.invoke("订阅列表")
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is SubscriptionContract.Effect.OnUnsubscribeResult -> {
                        snackbarHostState.showSnackbar(effect.message ?: "未知错误")
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { viewModel.onEvent(Event.OnShowSubscriptionIdDialog) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置订阅ID"
                        )
                    }
                }
            ) { innerPadding ->
                when {
                    state.isLoading -> Box(modifier = Modifier.padding(innerPadding)) {
                        SkeletonLoader()
                    }

                    state.error != null -> ErrorContent(
                        error = state.error!!,
                        onRetry = { viewModel.onEvent(Event.OnShowSubscriptionIdDialog) },
                        modifier = Modifier.padding(innerPadding)
                    )

                    else -> SubscriptionContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onThreadClicked = onThreadClicked,
                        onImageClick = { imgPath, ext ->
                            navigator.push(
                                ImagePreviewPage(
                                    imgPath = imgPath,
                                    ext = ext,
                                    hasNext = true,
                                    hasPrevious = true,
                                )
                            )
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        if (state.isShowSubscriptionIdDialog) {
            SubscriptionIdDialog(
                currentId = state.subscriptionId ?: "",
                onDismiss = { viewModel.onEvent(Event.OnHideSubscriptionIdDialog) },
                onConfirm = { id -> viewModel.onEvent(Event.OnSetSubscriptionId(id)) },
                onGenerateRandom = { viewModel.onEvent(Event.OnGenerateRandomSubscriptionId) }
            )
        }
    }
}

@Composable
private fun SubscriptionContent(
    state: SubscriptionContract.State,
    onEvent: (Event) -> Unit,
    onThreadClicked: (Long) -> Unit,
    onImageClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val feeds = state.feeds.collectAsLazyPagingItems()
    PullToRefreshWrapper(
        onRefreshTrigger = { feeds.refresh() },
        modifier = modifier
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp)
        ) {
            items(feeds.itemCount) { index ->
                val feed = feeds[index] ?: return@items
                SubscriptionCard(
                    feed = feed,
                    onClick = { onThreadClicked(feed.id) },
                    onImageClick = onImageClick,
                    onUnsubscribe = { onEvent(Event.OnUnsubscribe(feed.id)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                when {
                    feeds.loadState.append is LoadState.Error -> LoadingFailedIndicator()
                    feeds.loadState.append is LoadState.Loading -> LoadingIndicator()
                    feeds.loadState.append.endOfPaginationReached && feeds.itemCount == 0 -> EmptyContent()
                    feeds.loadState.append.endOfPaginationReached -> LoadEndIndicator()
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error.message ?: "未知错误",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("设置订阅ID")
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无订阅",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在帖子页面点击菜单可以添加订阅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubscriptionIdDialog(
    currentId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onGenerateRandom: () -> Unit
) {
    var subscriptionId by remember { mutableStateOf(currentId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置订阅ID") },
        text = {
            Column {
                Text(
                    "订阅ID用于标识您的订阅列表，可以自定义或随机生成。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = subscriptionId,
                    onValueChange = { subscriptionId = it },
                    label = { Text("订阅ID") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(subscriptionId) })
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(subscriptionId) }) {
                Text("确定")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(onClick = onGenerateRandom) {
                    Text("随机生成")
                }
            }
        }
    )
}
