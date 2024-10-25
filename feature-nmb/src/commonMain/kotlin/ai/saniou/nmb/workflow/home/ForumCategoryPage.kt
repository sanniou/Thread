package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.di.nmbdi
import ai.saniou.nmb.workflow.forum.Forum
import ai.saniou.nmb.workflow.forum.ForumViewModel
import ai.saniou.nmb.workflow.forum.ShowForumUiState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance


@Composable
fun ForumCategoryPage(di: DI = nmbdi, onThreadClicked: (Long) -> Unit) {
    val forumCategoryViewModel: ForumCategoryViewModel = viewModel {
        val forumCategoryViewModel by di.instance<ForumCategoryViewModel>()
        forumCategoryViewModel;
    }
    val content by forumCategoryViewModel.uiState.collectAsStateWithLifecycle()

    val forumViewModel: ForumViewModel = viewModel {
        val forumViewModel by di.instance<ForumViewModel>()
        forumViewModel;
    }
    val forumContent by forumViewModel.uiState.collectAsStateWithLifecycle()

    ForumCategoryUi(content, forumContent, forumViewModel, onThreadClicked)
}

@Composable
@Preview
fun ForumCategoryUi(
    uiState: ForumCategoryUiState,
    forumContent: UiStateWrapper,
    forumViewModel: ForumViewModel,
    onThreadClicked: (Long) -> Unit
) {
    MaterialTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    LazyColumn {
                        items(uiState.forums) { category ->
                            Text(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        uiState.onCategoryClick(category.id)
                                    }
                                    .padding(16.dp),
                                color = if (uiState.expandCategory == category.id) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                text = category.name
                            )
                            AnimatedVisibility(uiState.expandCategory == category.id) {
                                Column {
                                    category.forums.forEach { forum ->
                                        Text(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable {
                                                    uiState.onForumClick(forum.id)
                                                    forumViewModel.refreshForum(forum.id.toLong())
                                                }
                                                .padding(16.dp)
                                                .padding(start = 16.dp),
                                            color = if (uiState.currentForum == forum.id) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                            text = forum.name,
                                        )

                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            forumContent.LoadingWrapper<ShowForumUiState>(content = {
                Forum(it, onThreadClicked)
            }, onRetryClick = {

            })

        }
    }
}

