package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.StateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.MBToolbar
import ai.saniou.forum.workflow.forum.ForumPage
import ai.saniou.forum.workflow.home.ForumCategoryContract.Event
import ai.saniou.thread.domain.model.forum.Channel as Forum
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

object ForumListPage : Screen {

    @Composable
    override fun Content() {
        val viewModel: ForumCategoryViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                MBToolbar(
                    title = "论坛列表",
//                    navIcon = Icons.AutoMirrored.Filled.ArrowBack,
//                    onNavClick = { navigator.pop() }
                )
            }
        ) { padding ->
            StateLayout(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = state.categoriesState,
                onRetry = { viewModel.onEvent(Event.LoadCategories) },
            ) { forumGroups ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = Dimens.padding_medium)
                ) {
                    // 最近访问
                    item {
                        SectionTitle("最近访问")
                        // TODO: 实现最近访问逻辑
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.padding_large),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
                        ) {
                            items(forumGroups.flatMap { it.forums }.take(5)) { forum ->
                                ForumChip(forum = forum) {
                                    navigator.push(
                                        ForumPage(
                                            forumId = forum.id.toLong(),
                                            fgroupId = forum.groupId.toLong()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 收藏
                    item {
                        Spacer(modifier = Modifier.height(Dimens.padding_large))
                        val favoriteForums =
                            forumGroups.find { it.name == "收藏" }?.forums ?: emptyList()
                        if (favoriteForums.isNotEmpty()) {
                            SectionTitle("收藏")
                            ForumGrid(
                                forums = favoriteForums,
                                onForumClick = {
                                    navigator.push(
                                        ForumPage(
                                            forumId = it.id.toLong(),
                                            fgroupId = it.groupId.toLong()
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // 所有板块
                    forumGroups.filter { it.id.toLong() > 0 }.forEach { category ->
                        item(key = category.id) {
                            Spacer(modifier = Modifier.height(Dimens.padding_large))
                            SectionTitle(category.name)
                            ForumGrid(
                                forums = category.forums,
                                onForumClick = {
                                    navigator.push(
                                        ForumPage(
                                            forumId = it.id.toLong(),
                                            fgroupId = it.groupId.toLong()
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

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = Dimens.padding_large,
                vertical = Dimens.padding_small
            )
        )
    }

    @Composable
    private fun ForumChip(forum: Forum, onClick: () -> Unit) {
        Card(
            modifier = Modifier.clickable(onClick = onClick),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = forum.name,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(
                    horizontal = Dimens.padding_medium,
                    vertical = Dimens.padding_small
                )
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ForumGrid(forums: List<Forum>, onForumClick: (Forum) -> Unit) {
        FlowRow(
            modifier = Modifier.padding(horizontal = Dimens.padding_large),
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            forums.forEach { forum ->
                ForumGridItem(forum = forum, onClick = { onForumClick(forum) })
            }
        }
    }

    @Composable
    private fun ForumGridItem(forum: Forum, onClick: () -> Unit) {
        Card(
            modifier = Modifier.clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        ) {
            Row(
                modifier = Modifier.padding(Dimens.padding_medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = forum.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (forum.msg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = forum.msg.replace(Regex("<[^>]*>"), "").trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
