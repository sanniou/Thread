package ai.saniou.thread.feature.history

import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.widgets.ArticleItem
import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.history.HistoryArticle
import ai.saniou.thread.domain.model.history.HistoryPost
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.compose.localDI
import org.kodein.di.instance

class HistoryPage : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val viewModel: HistoryViewModel by localDI().instance()
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val historyItems = viewModel.historyItems.collectAsLazyPagingItems()
        val typeFilter by viewModel.typeFilter.collectAsState()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    SaniouTopAppBar(
                        title = "浏览历史",
                        onNavigationClick = { navigator.pop() },
                        scrollBehavior = scrollBehavior
                    )
                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = typeFilter == null,
                            onClick = { viewModel.onFilterChanged(null) },
                            label = { Text("全部") }
                        )
                        FilterChip(
                            selected = typeFilter == "post",
                            onClick = { viewModel.onFilterChanged("post") },
                            label = { Text("帖子") }
                        )
                        FilterChip(
                            selected = typeFilter == "article",
                            onClick = { viewModel.onFilterChanged("article") },
                            label = { Text("文章") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            PagingStateLayout(
                items = historyItems,
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                loading = {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                },
                empty = {
                    Text(
                        text = "没有浏览历史",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (index in 0 until historyItems.itemCount) {
                        val item = historyItems[index]
                        when (item) {
                            is HistoryUiItem.DateSeparator -> {
                                stickyHeader(key = item.date) {
                                    Text(
                                        text = item.date,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }

                            is HistoryUiItem.Item -> {
                                item(key = item.history.uniqueId) {
                                    when (val history = item.history) {
                                        is HistoryPost -> {
                                            TopicCard(
                                                topic = history.post,
                                                onClick = { navigator.push(TopicDetailPage(history.post.toString())) },
                                                // Simplified for history list
                                                onImageClick = { _ -> navigator.push(TopicDetailPage(history.post.toString())) },
                                                onUserClick = { }
                                            )
                                        }

                                        is HistoryArticle -> {
                                            ArticleItem(
                                                article = history.article,
                                                sourceName = history.article.feedSourceId, // TODO: Get real source name
                                                onClick = { navigator.push(ArticleDetailPage(history.article.id)) },
                                                showUnreadIndicator = false
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.padding(top = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }

                            null -> {
                                // Placeholder
                            }
                        }
                    }
                }
            }
        }
    }
}
