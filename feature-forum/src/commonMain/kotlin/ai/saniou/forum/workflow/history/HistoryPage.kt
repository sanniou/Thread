package ai.saniou.forum.workflow.history

import ai.saniou.coreui.widgets.SaniouTopAppBar
import ai.saniou.forum.workflow.home.ListThreadPage
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.history.HistoryPost
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.cash.paging.filter
import app.cash.paging.map
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.map
import org.kodein.di.compose.localDI
import org.kodein.di.instance

class HistoryPage : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: HistoryViewModel by localDI().instance()
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SaniouTopAppBar(
                    title = "浏览历史",
                    onNavigationClick = { navigator.pop() },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            // FIXME: ListThreadPage currently only supports Post.
            // We need to filter or map HistoryItem to Post for now, or update ListThreadPage to support HistoryItem.
            // Given the current task scope, we'll map and filter for Posts.
            // In the future, we should have a unified list component.
            val postFlow = viewModel.historyItems.map { pagingData ->
                pagingData.map {
                    (if (it is HistoryPost) it.post else null) as Topic
                }
            }

            // Note: PagingData.map doesn't support filtering nulls directly in a way that reduces count.
            // It maps T -> R. If R is null, it's still an item? No, PagingData.map transforms.
            // Actually PagingData.filter is available.
            // But here we have Flow<PagingData<HistoryItem>>.
            // We want Flow<PagingData<Post>>.
            // We can use `filter` then `map`.

            // However, `ListThreadPage` expects `Flow<PagingData<Post>>`.
            // Let's do the transformation properly.

            // Since we can't easily import `filter` and `map` extensions inside the Composable without correct imports,
            // and `ListThreadPage` might need refactoring to support mixed content.
            // For now, let's assume we only show Posts in this specific page or map them.

            // Actually, `ListThreadPage` takes `threadFlow: Flow<PagingData<Post>>`.
            // We need to transform `viewModel.historyItems` (Flow<PagingData<HistoryItem>>) to `Flow<PagingData<Post>>`.

            // We can do this in the ViewModel or here. Doing it here for now to keep ViewModel clean for generic history.

            // Wait, `PagingData` operations like `filter` and `map` are available as extension functions.
            // We need to import `app.cash.paging.filter` and `app.cash.paging.map`.

            // But `filter` on PagingData is `filter(predicate: suspend (T) -> Boolean)`.
            // `map` is `map(transform: suspend (T) -> R)`.

            // We can't easily "mapNotNull" on PagingData directly in one step if the library doesn't support it.
            // But we can filter then map.

            // Let's try to use `insertSeparators` or just `map` and handle nulls in UI?
            // `ListThreadPage` expects `Post`. It won't handle nulls.

            // Let's modify ViewModel to expose `historyPosts` for now, or do the transformation here.
            // Transformation here:
            /*
            val posts = viewModel.historyItems.map { pagingData ->
                pagingData
                    .filter { it is HistoryPost }
                    .map { (it as HistoryPost).post }
            }
            */
            // We need to make sure imports are correct.

            ListThreadPage(
                onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) },
                threadFlow = viewModel.historyItems.map { pagingData ->
                    pagingData
                        .filter { it is HistoryPost }
                        .map { (it as HistoryPost).post }
                },
                onThreadClicked = { navigator.push(TopicDetailPage(it)) },
                onImageClick = { _, img ->
                    navigator.push(
                        ImagePreviewPage(
                            ImagePreviewViewModelParams(
                                initialImages = listOf(img),
                            ),
                        )
                    )
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
