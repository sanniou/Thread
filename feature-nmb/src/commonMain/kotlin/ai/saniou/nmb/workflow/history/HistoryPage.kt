package ai.saniou.nmb.workflow.history

import ai.saniou.nmb.workflow.home.ListThreadPage
import ai.saniou.nmb.workflow.image.ImagePreviewPage
import ai.saniou.nmb.workflow.thread.ThreadPage
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
            threads = viewModel.historyThreads,
            onThreadClicked = { navigator.push(ThreadPage(it)) },
            onImageClick = { threadId, imgPath, ext ->
                navigator.push(
                    ImagePreviewPage(
                        threadId = threadId,
                        imgPath = imgPath,
                        ext = ext,
                    )
                )
            }
        )
    }
}
