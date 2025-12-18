package ai.saniou.forum.workflow.subscription

import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.state.DefaultError
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.ui.components.ForumThreadCard
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.forum.workflow.image.ImageInfo
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.subscription.SubscriptionContract.Event
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.DI
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.subscription_cancel
import thread.feature_forum.generated.resources.subscription_confirm
import thread.feature_forum.generated.resources.subscription_empty
import thread.feature_forum.generated.resources.subscription_empty_hint
import thread.feature_forum.generated.resources.subscription_empty_no_id
import thread.feature_forum.generated.resources.subscription_empty_no_id_hint
import thread.feature_forum.generated.resources.subscription_generate_random
import thread.feature_forum.generated.resources.subscription_id_dialog_message
import thread.feature_forum.generated.resources.subscription_id_dialog_title
import thread.feature_forum.generated.resources.subscription_id_empty_error
import thread.feature_forum.generated.resources.subscription_id_label
import thread.feature_forum.generated.resources.subscription_local_label
import thread.feature_forum.generated.resources.subscription_pull_cloud
import thread.feature_forum.generated.resources.subscription_push_local
import thread.feature_forum.generated.resources.subscription_set_id
import thread.feature_forum.generated.resources.subscription_title
import thread.feature_forum.generated.resources.subscription_unknown_error
import thread.feature_forum.generated.resources.subscription_unsubscribe

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
            onUpdateTitle?.invoke(getString(Res.string.subscription_title))
        }

        LaunchedEffect(viewModel.effect) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is SubscriptionContract.Effect.OnUnsubscribeResult -> {
                        snackbarHostState.showSnackbar(effect.message ?: getString(Res.string.subscription_unknown_error))
                    }

                    is SubscriptionContract.Effect.OnPushResult -> {
                        snackbarHostState.showSnackbar(effect.message ?: getString(Res.string.subscription_unknown_error))
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    SaniouTopAppBar(
                        title = stringResource(Res.string.subscription_title),
                        onNavigationClick = { navigator.pop() },
                        actions = {
                            IconButton(onClick = { viewModel.onEvent(Event.OnPull) }) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = stringResource(Res.string.subscription_pull_cloud)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.onEvent(Event.OnPush) },
                                enabled = state.isPushEnabled
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = stringResource(Res.string.subscription_push_local)
                                )
                            }
                            IconButton(onClick = { viewModel.onEvent(Event.OnShowSubscriptionIdDialog) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(Res.string.subscription_set_id)
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
                            navigator.push(
                                ImagePreviewPage(
                                    ImagePreviewViewModelParams(
                                        initialImages = listOf(imageInfo),
                                    ),
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
        PagingStateLayout(
            items = feeds,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            },
            empty = { EmptyContent(hasId = state.subscriptionId != null) }
        ) {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(feeds.itemCount) { index ->
                    val feed = feeds[index] ?: return@items
                    var showMenu by remember { mutableStateOf(false) }
                    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

                    Box(
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    showMenu = true
                                    pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                                },
                                onTap = {
                                    onThreadClicked(feed.id.toLong())
                                }
                            )
                        }
                    ) {
                        ForumThreadCard(
                            thread = feed,
                            onClick = { onThreadClicked(feed.id.toLong()) },
                            onImageClick = { img, ext -> onImageClick(feed.id.toLong(), img, ext) },
                        )
                        if (feed.isLocal) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(Res.string.subscription_local_label),
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = pressOffset
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.subscription_unsubscribe)) },
                                onClick = {
                                    onEvent(Event.OnUnsubscribe(feed.id.toLong()))
                                    showMenu = false
                                }
                            )
                        }
                    }
                }

                item {
                    when {
                        feeds.loadState.append is LoadStateError -> LoadingFailedIndicator()
                        feeds.loadState.append is LoadStateLoading -> LoadingIndicator()
                        feeds.loadState.append.endOfPaginationReached -> LoadEndIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        DefaultError(error = error, onRetryClick = onRetry)
    }
}

@Composable
private fun EmptyContent(
    hasId: Boolean,
    modifier: Modifier = Modifier
) {
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
                text = stringResource(if (hasId) Res.string.subscription_empty else Res.string.subscription_empty_no_id),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(if (hasId) Res.string.subscription_empty_hint else Res.string.subscription_empty_no_id_hint),
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
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.subscription_id_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.subscription_id_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = subscriptionId,
                    onValueChange = {
                        subscriptionId = it
                        isError = false
                    },
                    label = { Text(stringResource(Res.string.subscription_id_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(Res.string.subscription_id_empty_error)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (subscriptionId.isBlank()) {
                            isError = true
                        } else {
                            onConfirm(subscriptionId)
                        }
                    })
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (subscriptionId.isBlank()) {
                    isError = true
                } else {
                    onConfirm(subscriptionId)
                }
            }) {
                Text(stringResource(Res.string.subscription_confirm))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onGenerateRandom) {
                    Text(stringResource(Res.string.subscription_generate_random))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.subscription_cancel))
                }
            }
        }
    )
}
