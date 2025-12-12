package ai.saniou.forum.workflow.history

import ai.saniou.forum.workflow.home.ListThreadPage
import ai.saniou.forum.workflow.image.ImageInfo
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.thread.ThreadPage
import ai.saniou.forum.workflow.user.UserDetailPage
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.compose.localDI
import org.kodein.di.instance

class HistoryPage : Screen {
    @Composable
    override fun Content() {
        val viewModel: HistoryViewModel by localDI().instance()
        val navigator = LocalNavigator.currentOrThrow

        ListThreadPage(
            onUserClick = { userHash -> navigator.push(UserDetailPage(userHash)) },
            threadFlow = viewModel.historyThreads,
            onThreadClicked = { navigator.push(ThreadPage(it)) },
            onImageClick = { _, imgPath, ext ->
                val imageInfo = ImageInfo(imgPath, ext)
                navigator.push(
                    ImagePreviewPage(
                        ImagePreviewViewModelParams(
                            initialImages = listOf(imageInfo),
                        ),
                    )
                )
            }
        )
    }
}
