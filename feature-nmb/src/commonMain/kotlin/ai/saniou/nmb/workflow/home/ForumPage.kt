package ai.saniou.nmb.workflow.home

import ai.saniou.coreui.state.LoadingWrapper
import ai.saniou.nmb.di.nmbdi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.kodein.di.DI
import org.kodein.di.instance


@Composable
fun ForumScreen(
    di: DI = nmbdi,
    onThreadClicked: (Long) -> Unit = {},
) {
    val forumViewModel: ForumViewModel = viewModel {
        val forumCategoryViewModel by di.instance<ForumViewModel>()
        forumCategoryViewModel;
    }
    val forumContent by forumViewModel.uiState.collectAsStateWithLifecycle()
    forumContent.LoadingWrapper<ShowForumUiState>(content = {
        Forum(it, onThreadClicked)
    }, onRetryClick = {

    })

}

@Composable
@Preview
fun Forum(uiState: ShowForumUiState, onThreadClicked: (Long) -> Unit) {

    LazyColumn {
        items(uiState.showF) { forum ->
            Box(modifier = Modifier.fillMaxWidth()
                .clickable {
                    onThreadClicked(forum.id)
                }
            ) {
                Row {
                    Text(forum.sage.toString())
                    Text(forum.admin.toString())
                    Spacer(modifier = Modifier.weight(1f))
                    Text(forum.replyCount.toString())
                    Text(forum.remainReplies.toString())
                }
                Column {
                    Text(forum.title)
                    Text(forum.name)
                    Text(forum.content)
                    Text(forum.replies.toString())
                }
            }
        }
    }
}



