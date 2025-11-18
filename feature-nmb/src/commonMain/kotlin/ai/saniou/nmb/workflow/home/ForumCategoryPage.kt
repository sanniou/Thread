package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.ui.components.DrawerHeader
import ai.saniou.nmb.ui.components.DrawerMenuItem
import ai.saniou.nmb.ui.components.DrawerMenuRow
import ai.saniou.nmb.workflow.forum.ForumPage
import ai.saniou.nmb.workflow.home.ForumCategoryContract.Event
import ai.saniou.nmb.workflow.home.ForumCategoryContract.State
import ai.saniou.nmb.workflow.subscription.SubscriptionPage
import ai.saniou.nmb.workflow.thread.ThreadPage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.compose.viewmodel.rememberViewModel
import org.kodein.di.instance

@Composable
fun ForumCategoryPage(
    di: DI = nmbdi,
    drawerState: DrawerState,
) {
    val viewModel: ForumCategoryViewModel by rememberViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val greetImageViewModel: GreetImageViewModel by rememberViewModel()
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
                            .background(Color.White.copy(alpha = 0.8f))
                    ) {
                        DrawerMenuRow(
                            menuItems = listOf(
                                DrawerMenuItem(Icons.Default.Favorite, "订阅列表") {
                                    navigator.push(SubscriptionPage { threadId ->
                                        navigator.push(ThreadPage(threadId))
                                    })
                                    scope.launch { drawerState.close() }
                                },
                                DrawerMenuItem(Icons.Default.Home, "访问历史") { /* TODO */ },
                                DrawerMenuItem(Icons.Default.Send, "发言记录") { /* TODO */ },
                                DrawerMenuItem(Icons.Default.Search, "搜索") { /* TODO */ }
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
                                items(state.categories, key = { it.name }) { category ->
                                    CategoryItem(
                                        category = category,
                                        isExpanded = state.expandedCategoryId == category.id,
                                        onCategoryClick = {
                                            viewModel.onEvent(
                                                Event.ToggleCategory(
                                                    category.id
                                                )
                                            )
                                        },
                                        onCategoryLongClick = {
                                            // Consume long click
                                        }
                                    )
                                    AnimatedVisibility(visible = state.expandedCategoryId == category.id) {
                                        Column {
                                            category.forums.forEach { forum ->
                                                ForumItem(
                                                    forum = forum,
                                                    isSelected = state.currentForum?.id == forum.id,
                                                    isFavorite = state.favoriteForumIds.contains(
                                                        forum.id
                                                    ),
                                                    onForumClick = {
                                                        viewModel.onEvent(Event.SelectForum(forum))
                                                        scope.launch { drawerState.close() }
                                                    },
                                                    onFavoriteToggle = {
                                                        viewModel.onEvent(
                                                            Event.ToggleFavorite(
                                                                forum
                                                            )
                                                        )
                                                    }
                                                )
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
            ForumPage(forumId = forum.id, fgroupId = forum.fGroup).Content()
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("请从左侧选择一个板块")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryItem(
    category: ForumCategory,
    isExpanded: Boolean,
    onCategoryClick: () -> Unit,
    onCategoryLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCategoryClick,
                onLongClick = onCategoryLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon =
            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight
        val color =
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = category.name, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ForumItem(
    forum: ai.saniou.nmb.data.entity.ForumDetail,
    isSelected: Boolean,
    isFavorite: Boolean,
    onForumClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onForumClick,
                onLongClick = onFavoriteToggle
            )
            .padding(start = 32.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        val color =
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = if (forum.showName.isNullOrBlank()) forum.name else forum.showName,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "已收藏",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = forum.threadCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
