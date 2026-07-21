package ai.saniou.thread.feature.history

import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.ArticleItem
import ai.saniou.coreui.widgets.ContextHero
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.ThreadContentColumn
import ai.saniou.coreui.widgets.ThreadFilterBar
import ai.saniou.coreui.widgets.ThreadLoadingState
import ai.saniou.coreui.widgets.ThreadPage
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.reader.workflow.articledetail.ArticleDetailPage
import ai.saniou.thread.domain.model.history.HistoryArticle
import ai.saniou.thread.domain.model.history.HistoryPost
import ai.saniou.thread.FeedTopicRoute
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
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
        val historyItems = viewModel.historyItems.collectAsLazyPagingItems()
        val typeFilter by viewModel.typeFilter.collectAsState()

        ThreadPage {
            ThreadContentColumn(modifier = Modifier.fillMaxSize()) {
                ContextHero(
                    icon = Icons.Default.History,
                    title = "浏览历史",
                    subtitle = "按时间回到最近看过的帖子和文章",
                    metric = if (historyItems.itemCount > 0) {
                        "${historyItems.itemCount} 条最近"
                    } else {
                        "本地优先"
                    },
                )
                ThreadFilterBar(
                    items = listOf(null, "post", "article"),
                    selected = typeFilter,
                    label = {
                        when (it) {
                            "post" -> "帖子"
                            "article" -> "文章"
                            else -> "全部"
                        }
                    },
                    onSelect = viewModel::onFilterChanged,
                )
                PagingStateLayout(
                    items = historyItems,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    loading = {
                        ThreadLoadingState(modifier = Modifier.align(Alignment.Center))
                    },
                    empty = {
                        ModernEmptyState(
                            icon = Icons.Default.History,
                            title = "还没有浏览历史",
                            description = "阅读帖子或文章后，最近访问会按日期整理在这里。",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = Dimens.page_vertical),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                                    onClick = {
                                                        navigator.push(FeedTopicRoute(history.post.sourceId, history.post.id))
                                                    },
                                                    onImageClick = { _ ->
                                                        navigator.push(FeedTopicRoute(history.post.sourceId, history.post.id))
                                                    },
                                                    onUserClick = { },
                                                )
                                            }

                                            is HistoryArticle -> {
                                                ArticleItem(
                                                    article = history.article,
                                                    sourceName = history.sourceName,
                                                    onClick = { navigator.push(ArticleDetailPage(history.article.id)) },
                                                    showUnreadIndicator = false,
                                                )
                                            }
                                        }
                                    }
                                }

                                null -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
