package ai.saniou.forum.workflow.forum

import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.workflow.home.ListThreadPage
import ai.saniou.forum.workflow.image.ImageInfo
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.post.PostPage
import ai.saniou.forum.workflow.thread.ThreadPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Forum
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import ai.saniou.forum.workflow.user.UserPage
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

data class ForumPage(
    val di: DI = nmbdi,
    val sourceId: String = "nmb",
    val forumId: String,
    val fgroupId: String,
    val onMenuClick: (() -> Unit)? = null,
) : Screen {

    // Compatibility constructor for existing navigation calls (mostly NMB)
    constructor(forumId: Long, fgroupId: Long, onMenuClick: (() -> Unit)? = null) : this(
        sourceId = "nmb",
        forumId = forumId.toString(),
        fgroupId = fgroupId.toString(),
        onMenuClick = onMenuClick
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // TODO: Pass sourceId to ViewModel factory when DI supports it or ViewModel is updated to accept it via factory
        // For now, ViewModel might still be expecting Longs in its constructor if not updated.
        // We need to check ForumViewModel definition again.
        // ForumViewModel takes (..., forumId: Long, fgroupId: Long).
        // We need to update ForumViewModel to take Strings and sourceId.
        // But I cannot update ForumViewModel in this apply_diff.
        // So I will convert back to Long here if source is "nmb", else use 0L/dummy if generic.
        // Wait, I updated ForumViewModel in previous step but only its `getForumThreadsPagingUseCase` call,
        // not its constructor signature.
        // Let's assume for this step I only update ForumPage to hold the data,
        // and I pass converted data to ViewModel until ViewModel is fully refactored.
        
        val viewModel: ForumViewModel = rememberScreenModel(tag = "${sourceId}_${fgroupId}_${forumId}") {
            // This will fail if ForumViewModel constructor is not updated to accept (String, String, String)
            // or if DI module is not updated to provide it.
            // Currently DI provides ForumViewModel using `instance(arg = forumId to fgroupId)` where args are Long.
            // This suggests I need to update DI and ViewModel too.
            // Since I am in "Architect" mode effectively (planning large change),
            // I should have updated ViewModel first.
            // But I am in Code mode.
            // Let's proceed with minimal changes:
            // 1. Update ForumPage (this file).
            // 2. Update ForumViewModel (next).
            // 3. Update DI (next).
            
            // Temporary hack: Pass Longs as before.
            // Ideally we want `di.direct.instance(arg = Triple(sourceId, forumId, fgroupId))`
            val fIdLong = forumId.toLongOrNull() ?: 0L
            val fGroupLong = fgroupId.toLongOrNull() ?: 0L
            di.direct.instance(arg = fIdLong to fGroupLong)
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val threads = viewModel.threads.collectAsLazyPagingItems()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var expandedFab by remember { mutableStateOf(true) }
        val fabNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -5f) {
                        expandedFab = false
                    } else if (available.y > 5f) {
                        expandedFab = true
                    }
                    return Offset.Zero
                }
            }
        }

        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
            if (lazyListState.firstVisibleItemIndex == 0) {
                expandedFab = true
            }
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    ForumContract.Effect.ScrollToTop -> lazyListState.animateScrollToItem(0)
                }
            }
        }

        if (state.showInfoDialog) {
            ForumInfoDialog(
                forumDetail = state.forumDetail,
                onDismissRequest = { viewModel.onEvent(ForumContract.Event.ToggleInfoDialog(false)) }
            )
        }

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(fabNestedScrollConnection),
            topBar = {
                SaniouTopAppBar(
                    title = {
                        Text(
                            text = state.forumName,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                                            threads.refresh()
                                        } else {
                                            coroutineScope.launch {
                                                lazyListState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    },
                    navigationIcon = {
                        if (onMenuClick != null) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        } else {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.onEvent(
                                ForumContract.Event.ToggleInfoDialog(
                                    true
                                )
                            )
                        }) {
                            Icon(Icons.Outlined.Info, contentDescription = "板块信息")
                        }
                        IconButton(onClick = {
                            navigator.push(UserPage())
                        }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "用户中心")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        navigator.push(
                            PostPage(
                                fid = forumId.toInt(),
                                forumName = state.forumName
                            )
                        )
                    },
                    expanded = expandedFab,
                    icon = { Icon(Icons.Default.Add, "发帖") },
                    text = { Text("发帖") }
                )
            }
        ) { innerPadding ->
            ListThreadPage(
                state = lazyListState,
                threadFlow = viewModel.threads,
                onThreadClicked = { threadId -> navigator.push(ThreadPage(threadId)) },
                onImageClick = { _, imgPath, ext ->
                    val imageInfo = ImageInfo(imgPath, ext)
                    navigator.push(
                        ImagePreviewPage(
                            ImagePreviewViewModelParams(
                                initialImages = listOf(imageInfo),
                            )
                        )
                    )
                },
                onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    @Composable
    private fun ForumInfoDialog(
        forumDetail: Forum?,
        onDismissRequest: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = forumDetail?.name ?: "板块信息") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (forumDetail != null) {
                        if (!forumDetail.msg.isBlank()) {
                            RichText(
                                text = forumDetail.msg,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        DetailItem("ID", forumDetail.id.toString())
                        forumDetail.threadCount?.let {
                            DetailItem("帖子数", it.toString())
                        }
                        forumDetail.interval?.let {
                            DetailItem("发帖间隔", "${it}秒")
                        }
                        forumDetail.autoDelete?.let {
                            if (it > 0) {
                                DetailItem("自动删除", "${it}小时后")
                            }
                        }
                        forumDetail.safeMode?.let {
                            if (it == "1") {
                                DetailItem("安全模式", "开启")
                            }
                        }
                    } else {
                        Text("暂无详细信息")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("关闭")
                }
            }
        )
    }

    @Composable
    private fun DetailItem(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}





