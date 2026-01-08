package ai.saniou.forum.workflow.topicdetail

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.Effect
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.Event
import ai.saniou.forum.workflow.topicdetail.TopicDetailContract.State
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.usecase.bookmark.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.subscription.GetActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.IsSubscribedUseCase
import ai.saniou.thread.domain.usecase.subscription.ToggleSubscriptionUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicCommentsPagerUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicDetailUseCase
import ai.saniou.thread.domain.usecase.thread.GetSubCommentsUseCase
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.usecase.thread.UpdateTopicLastAccessTimeUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateTopicLastReadCommentIdUseCase
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class TopicDetailViewModelParams(
    val sourceId: String,
    val topicId: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TopicDetailViewModel(
    params: TopicDetailViewModelParams,
    private val getTopicDetailUseCase: GetTopicDetailUseCase,
    private val getTopicCommentsPagerUseCase: GetTopicCommentsPagerUseCase,
    private val getSubCommentsUseCase: GetSubCommentsUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val getActiveSubscriptionKeyUseCase: GetActiveSubscriptionKeyUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val getChannelNameUseCase: GetChannelNameUseCase,
    private val updateTopicLastAccessTimeUseCase: UpdateTopicLastAccessTimeUseCase,
    private val updateTopicLastReadCommentIdUseCase: UpdateTopicLastReadCommentIdUseCase,
    private val cdnManager: CdnManager,
) : ScreenModel {

    private val sourceId = params.sourceId
    private val threadId = params.topicId

    private data class LoadRequest(
        val threadId: String,
        val isPoOnly: Boolean = false,
        val page: Int = 1,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadRequest = MutableStateFlow(LoadRequest(threadId = threadId))

    val replies: Flow<PagingData<Comment>> =
        loadRequest.flatMapLatest { request ->
            getTopicCommentsPagerUseCase(
                sourceId = sourceId,
                topicId = request.threadId,
                isPoOnly = request.isPoOnly
            )
        }.cachedIn(screenModelScope)

    init {
        observeTopicDetail()
        updateLastAccessTime()
        // _state.update { it.copy(replies = replies) } // replies is collected in UI

        screenModelScope.launch {
            observeSubscriptionStatus()
            loadRequest.collect { request ->
                // _state.update { it.copy(isPoOnlyMode = request.isPoOnly) } // isPoOnlyMode removed from State? Check Contract
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.ShowSubComments -> showSubComments(event.commentId)
            Event.HideSubComments -> _state.update { it.copy(showSubCommentsDialog = false) }
            Event.RetryTopicLoad -> observeTopicDetail(forceRefresh = true)
            Event.RetrySubCommentsLoad -> state.value.activeCommentId?.let { showSubComments(it) }
            // ... other events need to be mapped or removed if not in Contract
            else -> {}
        }
    }

    private fun showSubComments(commentId: String) {
        _state.update {
            it.copy(
                showSubCommentsDialog = true,
                activeCommentId = commentId,
                subCommentsWrapper = UiStateWrapper.Loading
            )
        }
        screenModelScope.launch {
            getSubCommentsUseCase(sourceId, threadId, commentId, 1)
                .onSuccess { comments ->
                    _state.update { it.copy(subCommentsWrapper = UiStateWrapper.Success(comments)) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(subCommentsWrapper = UiStateWrapper.Error(e.toAppError {
                            showSubComments(commentId)
                        }))
                    }
                }
        }
    }

    private fun observeTopicDetail(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.update { it.copy(topicWrapper = UiStateWrapper.Loading) }
            getTopicDetailUseCase(sourceId, threadId, forceRefresh)
                .catch { e ->
                    _state.update {
                        it.copy(topicWrapper = UiStateWrapper.Error(e.toAppError {
                            observeTopicDetail(forceRefresh = true)
                        }))
                    }
                }
                .collectLatest { topic ->
                    _state.update {
                        it.copy(topicWrapper = UiStateWrapper.Success(topic))
                    }
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun updateLastAccessTime() {
        val tid = threadId.toLongOrNull() ?: return
        screenModelScope.launch {
            updateTopicLastAccessTimeUseCase(
                sourceId,
                tid.toString(),
                Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private fun updateLastReadReplyId(replyId: String) {
        val tid = threadId.toLongOrNull() ?: return
        screenModelScope.launch {
            updateTopicLastReadCommentIdUseCase(sourceId, tid.toString(), replyId)
        }
    }

    // Removed observeTopicMetadata as it is redundant with observeTopicDetail which fetches full Topic.
    // Topic contains metadata info.
    // If separate metadata fetching is needed, it should update specific fields in State,
    // but currently State uses UiStateWrapper<Topic> as the main source of truth.

    private fun observeSubscriptionStatus() {
        screenModelScope.launch {
            val subscriptionKey = getActiveSubscriptionKeyUseCase() ?: return@launch
            isSubscribedUseCase(subscriptionKey, threadId).collect { isSubscribed ->
                _state.update { it.copy(isSubscribed = isSubscribed) }
            }
        }
    }

    private fun toggleSubscription() {
        if (state.value.isTogglingSubscription) return
//        val metadata = state.value.metadata ?: return
        val metadata = (state.value.topicWrapper as? UiStateWrapper.Success)?.value ?: return

        _state.update { it.copy(isTogglingSubscription = true) }

        val currentSubscribed = state.value.isSubscribed
        screenModelScope.launch {
            val subscriptionKey =
                getActiveSubscriptionKeyUseCase() ?: throw IllegalStateException("未设置订阅ID")
            toggleSubscriptionUseCase(subscriptionKey, metadata.id, currentSubscribed)
                .onSuccess { resultMessage ->
                    // UI state will be updated by the database flow
                    _state.update { it.copy(isTogglingSubscription = false) }
                    _effect.send(Effect.ShowSnackbar(resultMessage))
                }
                .onFailure { e ->
                    _state.update { it.copy(isTogglingSubscription = false) }
                    _effect.send(Effect.ShowSnackbar("操作失败: ${e.message}"))
                }
        }
    }

    private fun copyLink() {
        screenModelScope.launch {
            val url = "https://nmb.com/t/$threadId"
            _effect.send(Effect.CopyToClipboard(url))
            _effect.send(Effect.ShowSnackbar("链接已复制到剪贴板"))
        }
    }

    private fun copyContent(content: String) {
        screenModelScope.launch {
            _effect.send(Effect.CopyToClipboard(content))
            _effect.send(Effect.ShowSnackbar("内容已复制到剪贴板"))
        }
    }

    private fun bookmarkThread(metadata: TopicMetadata) {
        screenModelScope.launch {
            // Cannot bookmark a thread without its content.
            // This action should be disabled or get content from somewhere else.
            // For now, we just show a message.
            _effect.send(Effect.ShowSnackbar("无法收藏：缺少主楼内容"))
        }
    }

    private fun bookmarkReply(reply: Comment) {
        screenModelScope.launch {
            addBookmarkUseCase(
                Bookmark.Quote(
                    id = "nmb.ThreadReply.${reply.id}",
                    createdAt = Clock.System.now(),
                    tags = listOf(),
                    content = reply.content,
                    sourceId = reply.id.toString(),
                    sourceType = "nmb.ThreadReply"
                )
            )
            _effect.send(Effect.ShowSnackbar("回复已收藏"))
        }
    }

    private fun bookmarkImage(image: Image) {
        screenModelScope.launch {
            val fullUrl = cdnManager.buildImageUrl(image.originalUrl, "", isThumb = false)
            val id = "nmb.Image.${fullUrl.hashCode()}"
            addBookmarkUseCase(
                Bookmark.Image(
                    id = id,
                    createdAt = Clock.System.now(),
                    tags = listOf(),
                    url = fullUrl,
                    width = null,
                    height = null
                )
            )
            _effect.send(Effect.ShowSnackbar("图片已收藏"))
        }
    }

    private fun showImagePreview() {
        screenModelScope.launch {
            _effect.send(Effect.NavigateToImagePreview)
        }
    }
}
