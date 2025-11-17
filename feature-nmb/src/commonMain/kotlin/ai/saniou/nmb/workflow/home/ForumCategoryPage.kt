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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.kodein.di.instance

@Composable
fun ForumCategoryPage(
    di: DI = nmbdi,
    drawerState: DrawerState,
) {
    val viewModel: ForumCategoryViewModel = viewModel {
        val vm by di.instance<ForumCategoryViewModel>()
        vm
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val greetImageViewModel: GreetImageViewModel = viewModel {
        val vm by di.instance<GreetImageViewModel>()
        vm
    }
    val greetImageUrl by greetImageViewModel.greetImageUrl.collectAsStateWithLifecycle()

    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    val favoriteCategory = remember(state.favoriteForums) {
        ForumCategory(
            id = -1L, // 使用一个固定的特殊ID
            sort = -1L,
            name = "收藏",
            status = "n",
            forums = state.favoriteForums
        )
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
                                val categories = buildList {
                                    add(favoriteCategory)
                                    addAll(state.categories)
                                }
                                items(categories, key = { it.name }) { category ->
                                    CategoryItem(
                                        category = category,
                                        isExpanded = state.expandedCategoryId == category.id,
                                        onCategoryClick = {
                                            viewModel.onEvent(
                                                Event.ToggleCategory(
                                                    category.id
                                                )
                                            )
                                        }
                                    )
                                    AnimatedVisibility(visible = state.expandedCategoryId == category.id) {
                                        Column {
                                            category.forums.forEach { forum ->
                                                ForumItem(
                                                    forum = forum,
                                                    isSelected = state.currentForum?.id == forum.id,
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

@Composable
private fun CategoryItem(
    category: ForumCategory,
    isExpanded: Boolean,
    onCategoryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCategoryClick)
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

@Composable
private fun ForumItem(
    forum: ai.saniou.nmb.data.entity.ForumDetail,
    isSelected: Boolean,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Text(
            text = forum.threadCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


