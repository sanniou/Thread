package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.widgets.DrawerMenuItem
import ai.saniou.coreui.widgets.DrawerMenuRow
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.DrawerHeader
import ai.saniou.nmb.workflow.bookmark.BookmarkPage
import ai.saniou.nmb.workflow.forum.ForumPage
import ai.saniou.nmb.workflow.home.ForumCategoryContract.Event
import ai.saniou.nmb.workflow.home.ForumCategoryContract.ForumGroupUiState
import ai.saniou.nmb.workflow.search.SearchPage
import ai.saniou.nmb.workflow.subscription.SubscriptionPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import ai.saniou.thread.domain.model.forum.Forum
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI

data class ForumCategoryPage(
    val di: DI = nmbdi,
    val drawerState: DrawerState,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel: ForumCategoryViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val greetImageViewModel: GreetImageViewModel = rememberScreenModel()
        val greetImageUrl by greetImageViewModel.greetImageUrl.collectAsStateWithLifecycle()

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.toastMessage) {
            state.toastMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.onEvent(Event.ToastShown)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DrawerHeader(imageUrl = greetImageUrl)
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .padding(top = 140.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            DrawerMenuRow(
                                menuItems = listOf(
                                    DrawerMenuItem(Icons.Default.Favorite, "订阅") {
                                        navigator.push(SubscriptionPage { threadId ->
                                            navigator.push(ThreadPage(threadId))
                                        })
                                        scope.launch { drawerState.close() }
                                    },
                                    DrawerMenuItem(Icons.Default.Star, "收藏") {
                                        navigator.push(BookmarkPage)
                                        scope.launch { drawerState.close() }
                                    },
                                    DrawerMenuItem(Icons.Default.Home, "历史") { /* TODO */ },
                                    DrawerMenuItem(Icons.Default.Send, "发言") { /* TODO */ },
                                    DrawerMenuItem(Icons.Default.Search, "搜索") {
                                        navigator.push(SearchPage())
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            if (state.isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    state.forumGroups.forEach { group ->
                                        item(key = group.id) {
                                            CategoryHeader(
                                                group = group,
                                                isExpanded = state.expandedGroupId == group.id,
                                                onToggle = {
                                                    viewModel.onEvent(
                                                        Event.ToggleCategory(group.id)
                                                    )
                                                }
                                            )
                                        }

                                        item(key = "content_${group.id}") {
                                            AnimatedVisibility(
                                                visible = state.expandedGroupId == group.id,
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                Column {
                                                    group.forums.forEach { forum ->
                                                        ForumItem(
                                                            forum = forum,
                                                            isSelected = state.currentForum?.id == forum.id,
                                                            isFavorite = state.favoriteForumIds.contains(forum.id),
                                                            onForumClick = {
                                                                viewModel.onEvent(Event.SelectForum(forum))
                                                                scope.launch { drawerState.close() }
                                                            },
                                                            onFavoriteToggle = {
                                                                viewModel.onEvent(Event.ToggleFavorite(forum))
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        ) {
            state.currentForum?.let { forum ->
                ForumPage(forumId = forum.id.toLong(), fgroupId = forum.groupId.toLong()).Content()
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "请从左侧选择一个板块",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun CategoryHeader(
        group: ForumGroupUiState,
        isExpanded: Boolean,
        onToggle: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
    @Composable
    private fun ForumItem(
        forum: Forum,
        isSelected: Boolean,
        isFavorite: Boolean,
        onForumClick: () -> Unit,
        onFavoriteToggle: () -> Unit,
    ) {
        val backgroundColor = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent

        val contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .combinedClickable(
                    onClick = onForumClick,
                    onLongClick = onFavoriteToggle
                )
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (forum.showName.isNullOrBlank()) forum.name else forum.showName!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )

                    // 状态图标
                    if (forum.autoDelete != null && forum.autoDelete!! > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "限时",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // 版规摘要 & 元数据
                if (forum.msg.isNotBlank() || forum.threadCount != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (forum.threadCount != null) {
                            Text(
                                text = "${forum.threadCount} 串",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize
                            )
                        }

                        // 简略显示 msg，去除 HTML 标签
                        val cleanMsg = forum.msg.replace(Regex("<[^>]*>"), "").trim()
                        if (cleanMsg.isNotBlank()) {
                            Text(
                                text = cleanMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                modifier = Modifier.weight(1f, fill = false) // 防止挤占
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
