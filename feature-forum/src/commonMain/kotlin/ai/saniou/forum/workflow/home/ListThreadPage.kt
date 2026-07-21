package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.DefaultError
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.theme.threadContentSizeSpec
import ai.saniou.coreui.widgets.ModernEmptyState
import androidx.compose.animation.animateContentSize
import ai.saniou.coreui.widgets.SaniouTextButton
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.label_show_cache
import ai.saniou.coreui.widgets.PullToRefreshWrapper
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import thread.feature_forum.generated.resources.s_4b3f9d3d5c
import thread.feature_forum.generated.resources.s_a7e12880b9

@Composable
fun ListThreadPage(
    threadFlow: Flow<PagingData<Topic>>,
    onThreadClicked: (String) -> Unit,
    onImageClick: (String, Image) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    headerContent: (@Composable () -> Unit)? = null,
    showChannelBadge: Boolean = true,
    onShowCache: (() -> Unit)? = null,
) {
    val threads = threadFlow.collectAsLazyPagingItems()
    val windowInfo = LocalThreadWindowInfo.current

    PullToRefreshWrapper(
        onRefreshTrigger = { threads.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            PagingStateLayout(
                items = threads,
                loading = { ThreadListSkeleton() },
                error = { appError ->
                    DefaultError(
                        error = appError,
                        onRetry = { threads.retry() },
                        action = {
                            if (onShowCache != null) {
                                SaniouTextButton(onClick = onShowCache, text = stringResource(Res.string.label_show_cache))
                            }
                        }
                    )
                },
                empty = {
                    ModernEmptyState(
                        icon = Icons.Outlined.Star,
                        title = stringResource(Res.string.s_a7e12880b9),
                        description = stringResource(Res.string.s_4b3f9d3d5c),
                        modifier = Modifier.align(Alignment.Center),
                    )
                },
                modifier = Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = Dimens.contentMaxWidth),
            ) {
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(
                        start = windowInfo.pageHorizontalPadding,
                        end = windowInfo.pageHorizontalPadding,
                        top = Dimens.padding_medium,
                        bottom = Dimens.page_vertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium)
                ) {
                    if (headerContent != null) {
                        item(key = "channel-header") { headerContent() }
                    }

                    items(
                        count = threads.itemCount,
                        key = threads.itemKey { "${it.sourceId}:${it.id}" },
                    ) { index ->
                        val feed = threads[index] ?: return@items
                        TopicCard(
                            topic = feed,
                            onClick = { onThreadClicked(feed.id) },
                            onImageClick = { img -> onImageClick(feed.id, img) },
                            onUserClick = onUserClick,
                            showChannelBadge = showChannelBadge,
                            modifier = Modifier.animateContentSize(animationSpec = threadContentSizeSpec()),
                        )
                    }

                    item(key = "paging-append") { PagingAppendState(threads) }
                }
            }
        }
    }
}
