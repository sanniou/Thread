package ai.saniou.forum.workflow.home

import ai.saniou.forum.workflow.subscription.SubscriptionPage
import ai.saniou.forum.workflow.thread.ThreadPage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.launch


class SubscriptionPaneScreen : Screen {
    @OptIn(
        ExperimentalMaterial3AdaptiveApi::class, ExperimentalComposeUiApi::class,
        ExperimentalMaterial3Api::class
    )
    @Composable
    override fun Content() {
        var threadId: Long? by rememberSaveable { mutableStateOf(null) }
        val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
        val scope = rememberCoroutineScope()
        val isListAndDetailVisible =
            navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded && navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

        BackHandler(enabled = navigator.canNavigateBack()) {
            scope.launch {
                navigator.navigateBack()
            }
        }

        val paneExpansionState = rememberPaneExpansionState(navigator.scaffoldValue)
        paneExpansionState.setFirstPaneProportion(0.5f)
        SharedTransitionLayout {
            AnimatedContent(targetState = isListAndDetailVisible) {
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    listPane = {
                        // val isDetailVisible = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                        SubscriptionPage(
                            onThreadClicked = {
                                threadId = it
                                scope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                }
                            }
                        ).Content()
                    },
                    detailPane = {
                        // val isDetailVisible = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                        threadId?.let { ThreadPage(it).Content() }
                    },
                    paneExpansionState = paneExpansionState,
                    paneExpansionDragHandle = { state ->
                        val interactionSource =
                            remember { MutableInteractionSource() }
                        DragHandle(
                            modifier =
                                Modifier.paneExpansionDraggable(
                                    state,
                                    LocalMinimumInteractiveComponentSize.current,
                                    interactionSource = interactionSource,
                                    semanticsProperties = {

                                    }
                                )
                        )
                    }
                )
            }
        }
    }
}
