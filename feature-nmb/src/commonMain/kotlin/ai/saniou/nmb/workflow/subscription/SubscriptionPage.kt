package ai.saniou.nmb.workflow.subscription

import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.ForumThreadCard
import ai.saniou.nmb.ui.components.LoadEndIndicator
import ai.saniou.nmb.ui.components.LoadingFailedIndicator
import ai.saniou.nmb.ui.components.LoadingIndicator
import ai.saniou.nmb.ui.components.ThreadListSkeleton
import ai.saniou.nmb.workflow.image.ImageInfo
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.image.ImagePreviewUiState
import ai.saniou.nmb.workflow.subscription.SubscriptionContract.Event
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import app.cash.paging.LoadState
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.DI

data class SubscriptionPage(
    val di: DI = nmbdi,
    val onUpdateTitle: ((String) -> Unit)? = null,
    val onThreadClicked: (Long) -> Unit,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }
        val viewModel: SubscriptionViewModel = rememberScreenModel()
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

                    is SubscriptionContract.Effect.OnPushResult -> {
                        snackbarHostState.showSnackbar(effect.message ?: "未知错误")
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("订阅列表") },
                        actions = {
                            IconButton(onClick = { viewModel.onEvent(Event.OnPull) }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "拉取云端订阅"
                                )
                            }
                            IconButton(
                                onClick = { viewModel.onEvent(Event.OnPush) },
                                enabled = state.isPushEnabled
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "推送本地订阅"
                                )
                            }
                            IconButton(onClick = { viewModel.onEvent(Event.OnShowSubscriptionIdDialog) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置订阅ID"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                when {
                    state.isLoading -> Box(modifier = Modifier.padding(innerPadding)) {
                        ThreadListSkeleton()
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
                        onImageClick = { _, imgPath, ext ->
                            val imageInfo = ImageInfo(imgPath, ext)
                            val uiState = ImagePreviewUiState(
                                images = listOf(imageInfo),
                                initialIndex = 0,
                                endReached = true
                            )
                            navigator.push(
                                ImagePreviewPage(
                                    uiState = uiState,
                                    onLoadMore = {}
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
    onImageClick: (Long, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feeds = state.feeds.collectAsLazyPagingItems()
    PullToRefreshWrapper(
        onRefreshTrigger = { feeds.refresh() },
        modifier = modifier
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(feeds.itemCount) { index ->
                val feed = feeds[index] ?: return@items
                Box {
                    ForumThreadCard(
                        thread = feed,
                        onClick = { onThreadClicked(feed.id) },
                        onImageClick = { img, ext -> onImageClick(feed.id, img, ext) },
//                    onUnsubscribe = { onEvent(Event.OnUnsubscribe(feed.id)) }
                    )
                    if (feed.isLocal) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "本地订阅",
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        )
                    }
                }
            }

            item {
                when {
                    feeds.loadState.append is LoadStateError -> LoadingFailedIndicator()
                    feeds.loadState.append is LoadStateLoading -> LoadingIndicator()
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
    modifier: Modifier = Modifier,
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
    onGenerateRandom: () -> Unit,
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
                TextButton(onClick = onGenerateRandom) {
                    Text("随机生成")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
