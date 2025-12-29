package ai.saniou.forum.workflow.search

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouSearchAppBar
import ai.saniou.forum.di.nmbdi
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.ui.components.LoadEndIndicator
import ai.saniou.forum.ui.components.LoadingFailedIndicator
import ai.saniou.forum.ui.components.LoadingIndicator
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.search.SearchContract.Event
import ai.saniou.forum.workflow.search.SearchContract.SearchType
import ai.saniou.forum.workflow.thread.ThreadPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.DI

data class SearchPage(
    val di: DI = nmbdi,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: SearchViewModel = rememberScreenModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                Column {
                    SaniouSearchAppBar(
                        query = state.query,
                        onQueryChange = { viewModel.onEvent(Event.QueryChanged(it)) },
                        onClearQuery = { viewModel.onEvent(Event.ClearQuery) },
                        onBack = { navigator.pop() },
                        placeholder = "搜索本地记录..."
                    )

                    // 类型筛选 Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchType.entries.forEach { type ->
                            FilterChip(
                                selected = state.searchType == type,
                                onClick = { viewModel.onEvent(Event.TypeChanged(type)) },
                                label = { Text(type.title) },
                                leadingIcon = if (state.searchType == type) {
                                    {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (state.query.isBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "输入关键词开始搜索",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (state.searchType == SearchType.THREAD) {
                        ThreadResultList(
                            viewModel = viewModel,
                            onThreadClick = { navigator.push(ThreadPage(it)) },
                            onImageClick = { threadId, img ->
                                ImagePreviewPage(
                                    ImagePreviewViewModelParams(
                                        initialImages = listOf(img),
                                    ),
                                )
                            },
                            onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) }
                        )
                    } else {
                        ReplyResultList(
                            viewModel = viewModel,
                            onThreadClick = { navigator.push(ThreadPage(it)) },
                            onImageClick = { threadId, img ->
                                ImagePreviewPage(
                                    ImagePreviewViewModelParams(
                                        initialImages = listOf(img),
                                    ),
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ThreadResultList(
        viewModel: SearchViewModel,
        onThreadClick: (String) -> Unit,
        onImageClick: (String, Image) -> Unit,
        onUserClick: (String) -> Unit,
    ) {
        val threads =
            viewModel.state.collectAsStateWithLifecycle().value.threadPagingData.collectAsLazyPagingItems()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(threads.itemCount) { index ->
                val thread = threads[index] ?: return@items
                TopicCard(
                    topic = thread,
                    onClick = { onThreadClick(thread.id) },
                    onImageClick = { img -> onImageClick(thread.id, img) },
                    onUserClick = onUserClick
                )
            }

            item {
                when (threads.loadState.append) {
                    is LoadStateError -> LoadingFailedIndicator()
                    is LoadStateLoading -> LoadingIndicator()
                    else -> if (threads.itemCount > 0) LoadEndIndicator() else null
                }
            }

            if (threads.loadState.refresh is LoadStateLoading) {
                item { LoadingIndicator() }
            }
        }
    }

    @Composable
    private fun ReplyResultList(
        viewModel: SearchViewModel,
        onThreadClick: (String) -> Unit,
        onImageClick: (String, Image) -> Unit,
    ) {
        val replies =
            viewModel.state.collectAsStateWithLifecycle().value.replyPagingData.collectAsLazyPagingItems()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(replies.itemCount) { index ->
                val reply = replies[index] ?: return@items
                SearchReplyCard(
                    reply = reply,
                    onClick = { onThreadClick(reply.topicId) },
                    onImageClick = { img -> onImageClick(reply.topicId, img) }
                )
            }

            item {
                when (replies.loadState.append) {
                    is LoadStateError -> LoadingFailedIndicator()
                    is LoadStateLoading -> LoadingIndicator()
                    else -> if (replies.itemCount > 0) LoadEndIndicator() else null
                }
            }

            if (replies.loadState.refresh is LoadStateLoading) {
                item { LoadingIndicator() }
            }
        }
    }
}

@Composable
fun SearchReplyCard(
    reply: Comment,
    onClick: () -> Unit,
    onImageClick: (Image) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.padding_medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
            ) {
                Text(
                    text = reply.author.id,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No.${reply.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = ">> No.${reply.topicId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (reply.title.isNullOrBlank().not()) {
                Text(
                    text = reply.title!!,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            RichText(
                text = reply.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                blankLinePolicy = BlankLinePolicy.REMOVE
            )
        }
    }
}
