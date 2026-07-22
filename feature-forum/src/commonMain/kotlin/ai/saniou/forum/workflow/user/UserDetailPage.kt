package ai.saniou.forum.workflow.user

import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.ui.components.ThreadListSkeleton
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.post.LocalAttachmentPicker
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.topicdetail.ThreadReply
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.compose.collectAsLazyPagingItems
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.coreui.widgets.ThreadLoadingState
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.compose.localDI
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.eyebrow_member_activity
import thread.feature_forum.generated.resources.empty_title
import thread.feature_forum.generated.resources.post_page_reply
import thread.feature_forum.generated.resources.retry
import thread.feature_forum.generated.resources.s_1f63c99409
import thread.feature_forum.generated.resources.s_2d0ce11e3d
import thread.feature_forum.generated.resources.s_76f1ed24cb
import thread.feature_forum.generated.resources.s_ad941b51d3
import thread.feature_forum.generated.resources.s_bdf32f0d53
import thread.feature_forum.generated.resources.s_dca79914e5
import thread.feature_forum.generated.resources.user_follow_count
import thread.feature_forum.generated.resources.user_fans_count
import thread.feature_forum.generated.resources.user_profile_loading
import thread.feature_forum.generated.resources.unfollow_user
import thread.feature_forum.generated.resources.follow_user
import thread.feature_forum.generated.resources.action_cancel
import thread.feature_forum.generated.resources.profile_save
import thread.feature_forum.generated.resources.profile_sex_female
import thread.feature_forum.generated.resources.profile_sex_male
import thread.feature_forum.generated.resources.profile_sex_unknown
import thread.feature_forum.generated.resources.profile_sex
import thread.feature_forum.generated.resources.profile_intro
import thread.feature_forum.generated.resources.profile_nick_name
import thread.feature_forum.generated.resources.edit_profile_title
import thread.feature_forum.generated.resources.edit_profile
import thread.feature_forum.generated.resources.profile_change_avatar
import thread.feature_forum.generated.resources.profile_upload_avatar
import thread.feature_forum.generated.resources.profile_avatar_pending
import thread.feature_forum.generated.resources.profile_avatar_clear

data class UserDetailPage(
    val sourceId: String,
    val userHash: String,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val di = localDI()
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: UserDetailViewModel = rememberScreenModel(tag = "$sourceId:$userHash") {
            di.direct.instance(arg = sourceId to userHash)
        }

        val state by viewModel.state.collectAsState()
        val pagerState = rememberPagerState(pageCount = { UserDetailContract.Tab.entries.size })
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val attachmentPicker = LocalAttachmentPicker.current

        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    UserDetailContract.Effect.NavigateBack -> navigator.pop()
                    UserDetailContract.Effect.RequestPortraitPicker -> {
                        val picker = attachmentPicker
                        if (picker == null) {
                            snackbarHostState.showSnackbar("当前平台暂不支持选择图片")
                            return@collectLatest
                        }
                        runCatching { picker.pickImage() }
                            .onSuccess { attachment ->
                                if (attachment != null) {
                                    viewModel.handleEvent(
                                        UserDetailContract.Event.PortraitPicked(
                                            fileName = attachment.fileName,
                                            bytes = attachment.bytes,
                                            contentType = attachment.contentType,
                                        )
                                    )
                                }
                            }
                            .onFailure { error ->
                                snackbarHostState.showSnackbar(
                                    error.message ?: "选择头像失败",
                                )
                            }
                    }
                }
            }
        }

        LaunchedEffect(state.actionMessage) {
            val message = state.actionMessage ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            viewModel.handleEvent(UserDetailContract.Event.ConsumeActionMessage)
        }

        LaunchedEffect(state.currentTab) {
            pagerState.animateScrollToPage(state.currentTab.ordinal)
        }

        LaunchedEffect(pagerState.currentPage) {
            viewModel.handleEvent(UserDetailContract.Event.SwitchTab(UserDetailContract.Tab.entries[pagerState.currentPage]))
        }

        val displayTitle = state.profile?.name?.takeIf { it.isNotBlank() } ?: userHash
        val subtitle = state.profile?.intro?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.s_dca79914e5)

        if (state.isEditDialogOpen) {
            EditProfileDialog(
                state = state,
                onNickNameChange = { viewModel.handleEvent(UserDetailContract.Event.EditNickNameChanged(it)) },
                onIntroChange = { viewModel.handleEvent(UserDetailContract.Event.EditIntroChanged(it)) },
                onSexChange = { viewModel.handleEvent(UserDetailContract.Event.EditSexChanged(it)) },
                onPickPortrait = { viewModel.handleEvent(UserDetailContract.Event.PickPortrait) },
                onUploadPortrait = { viewModel.handleEvent(UserDetailContract.Event.UploadPortrait) },
                onClearPortrait = { viewModel.handleEvent(UserDetailContract.Event.ClearPendingPortrait) },
                onDismiss = { viewModel.handleEvent(UserDetailContract.Event.DismissEditProfile) },
                onSubmit = { viewModel.handleEvent(UserDetailContract.Event.SubmitEditProfile) },
            )
        }

        ThreadDetailScaffold(
            title = displayTitle,
            eyebrow = stringResource(Res.string.eyebrow_member_activity),
            subtitle = subtitle,
            onBack = { viewModel.handleEvent(UserDetailContract.Event.Back) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues).fillMaxSize()) {
                if (state.supportsUserFollow || state.supportsProfileEdit) {
                    UserRelationHeader(
                        state = state,
                        onToggleFollow = { viewModel.handleEvent(UserDetailContract.Event.ToggleFollow) },
                        onEditProfile = { viewModel.handleEvent(UserDetailContract.Event.OpenEditProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = Dimens.contentMaxWidth)
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding, vertical = 8.dp),
                    )
                }
                SecondaryTabRow(
                    selectedTabIndex = state.currentTab.ordinal,
                    modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                        .align(Alignment.CenterHorizontally),
                ) {
                    UserDetailContract.Tab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = state.currentTab.ordinal == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = when (tab) {
                                        UserDetailContract.Tab.Topics -> stringResource(Res.string.s_1f63c99409)
                                        UserDetailContract.Tab.Comments -> stringResource(Res.string.post_page_reply)
                                    }
                                )
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth)
                        .align(Alignment.CenterHorizontally),
                ) { page ->
                    when (UserDetailContract.Tab.entries[page]) {
                    UserDetailContract.Tab.Topics -> {
                        state.topics?.let { flow ->
                            val topics = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                                    vertical = 12.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(topics.itemCount) { index ->
                                    val topic = topics[index]
                                    if (topic != null) {
                                        TopicCard(
                                            topic = topic,
                                            onClick = { navigator.push(TopicDetailPage(threadId = topic.id)) },
                                            onImageClick = { img ->
                                                navigator.push(
                                                    ImagePreviewPage(
                                                        ImagePreviewViewModelParams(
                                                            initialImages = listOf(img),
                                                        )
                                                    )
                                                )
                                            },
                                            modifier = threadAnimateItem(),
                                        )
                                    }
                                }

                                when (topics.loadState.refresh) {
                                    is Loading -> item { ThreadListSkeleton() }
                                    is Error -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SaniouButton(onClick = { topics.retry() }, text = stringResource(Res.string.retry))
                                        }
                                    }

                                    else -> {}
                                }

                                item { PagingAppendState(topics) }

                                if (topics.loadState.refresh !is Loading && topics.itemCount == 0) {
                                    item {
                                        EmptyContent(message = stringResource(Res.string.s_76f1ed24cb))
                                    }
                                }
                            }
                        }
                    }

                    UserDetailContract.Tab.Comments -> {
                        state.comments?.let { flow ->
                            val replies = flow.collectAsLazyPagingItems()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                                    vertical = 12.dp,
                                ),
                            ) {
                                items(replies.itemCount) { index ->
                                    val reply = replies[index]
                                    if (reply != null) {
                                        Column {
                                            val replyTitle = reply.title
                                            if (!replyTitle.isNullOrBlank() && replyTitle != stringResource(Res.string.empty_title)) {
                                                Text(
                                                    text = stringResource(Res.string.s_2d0ce11e3d, replyTitle),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(Res.string.s_bdf32f0d53, reply.topicId),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            }

                                            ThreadReply(
                                                reply = reply,
                                                poUserHash = "",
                                                onReplyClicked = { navigator.push(TopicDetailPage(threadId = reply.topicId)) },
                                                refClick = { navigator.push(TopicDetailPage(threadId = reply.topicId)) }, // 简化处理，暂时跳转到主串
                                                onImageClick = { img ->
                                                    navigator.push(
                                                        ImagePreviewPage(
                                                            ImagePreviewViewModelParams(
                                                                initialImages = listOf(img)),
                                                            )
                                                    )
                                                },
                                                onCopy = {},
                                                onBookmark = {},
                                                onUserClick = { userHash -> navigator.push(UserDetailPage(sourceId, userHash)) },
                                                onBookmarkImage = { _ -> }
                                            )
                                        }
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                when (replies.loadState.refresh) {
                                    is Loading -> item {
                                        ThreadLoadingState(modifier = Modifier.fillMaxWidth())
                                    }
                                    is Error -> item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SaniouButton(onClick = { replies.retry() }, text = stringResource(Res.string.retry))
                                        }
                                    }

                                    else -> {}
                                }

                                item { PagingAppendState(replies) }

                                if (replies.loadState.refresh !is Loading && replies.itemCount == 0) {
                                    item {
                                        EmptyContent(message = stringResource(Res.string.s_ad941b51d3))
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyContent(
        modifier: Modifier = Modifier,
        message: String,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserRelationHeader(
    state: UserDetailContract.State,
    onToggleFollow: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.isProfileLoading && state.profile == null) {
                Text(
                    text = stringResource(Res.string.user_profile_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val profile = state.profile
                if (profile != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        profile.fansCount?.let {
                            Text(
                                text = stringResource(Res.string.user_fans_count, it.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        profile.followCount?.let {
                            Text(
                                text = stringResource(Res.string.user_follow_count, it.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.supportsProfileEdit && state.isSelf) {
                        SaniouButton(
                            onClick = onEditProfile,
                            text = stringResource(Res.string.edit_profile),
                        )
                    }
                    if (state.supportsUserFollow && !state.isSelf) {
                        SaniouButton(
                            onClick = onToggleFollow,
                            enabled = !state.isFollowBusy,
                            text = if (state.profile?.isFollowing == true) {
                                stringResource(Res.string.unfollow_user)
                            } else {
                                stringResource(Res.string.follow_user)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    state: UserDetailContract.State,
    onNickNameChange: (String) -> Unit,
    onIntroChange: (String) -> Unit,
    onSexChange: (Int) -> Unit,
    onPickPortrait: () -> Unit,
    onUploadPortrait: () -> Unit,
    onClearPortrait: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    val busy = state.isSavingProfile || state.isUploadingPortrait
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(text = stringResource(Res.string.edit_profile_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val avatarUrl = state.profile?.avatar
                    if (!avatarUrl.isNullOrBlank()) {
                        NetworkImage(
                            imageUrl = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp),
                        ) {}
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SaniouTextButton(
                            onClick = onPickPortrait,
                            enabled = !busy,
                            text = stringResource(Res.string.profile_change_avatar),
                        )
                        val pending = state.pendingPortraitFileName
                        if (!pending.isNullOrBlank()) {
                            Text(
                                text = stringResource(Res.string.profile_avatar_pending, pending),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SaniouButton(
                                    onClick = onUploadPortrait,
                                    enabled = !busy,
                                    loading = state.isUploadingPortrait,
                                    text = stringResource(Res.string.profile_upload_avatar),
                                )
                                SaniouTextButton(
                                    onClick = onClearPortrait,
                                    enabled = !busy,
                                    text = stringResource(Res.string.profile_avatar_clear),
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = state.editNickName,
                    onValueChange = onNickNameChange,
                    singleLine = true,
                    label = { Text(stringResource(Res.string.profile_nick_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
                OutlinedTextField(
                    value = state.editIntro,
                    onValueChange = onIntroChange,
                    minLines = 3,
                    label = { Text(stringResource(Res.string.profile_intro)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
                Text(
                    text = stringResource(Res.string.profile_sex),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to Res.string.profile_sex_unknown,
                        1 to Res.string.profile_sex_male,
                        2 to Res.string.profile_sex_female,
                    ).forEach { (value, labelRes) ->
                        FilterChip(
                            selected = state.editSex == value,
                            onClick = { onSexChange(value) },
                            enabled = !busy,
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            SaniouButton(
                onClick = onSubmit,
                enabled = !busy && state.editNickName.isNotBlank(),
                loading = state.isSavingProfile,
                text = stringResource(Res.string.profile_save),
            )
        },
        dismissButton = {
            SaniouTextButton(
                onClick = onDismiss,
                enabled = !busy,
                text = stringResource(Res.string.action_cancel),
            )
        },
    )
}
