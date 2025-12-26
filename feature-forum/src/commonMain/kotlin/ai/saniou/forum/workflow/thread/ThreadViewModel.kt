package ai.saniou.forum.workflow.thread

import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.thread.ThreadContract.Effect
import ai.saniou.forum.workflow.thread.ThreadContract.Event
import ai.saniou.forum.workflow.thread.ThreadContract.State
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.model.forum.Comment as ThreadReply
import ai.saniou.thread.domain.usecase.bookmark.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.subscription.GetActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.IsSubscribedUseCase
import ai.saniou.thread.domain.usecase.subscription.ToggleSubscriptionUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicDetailUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicCommentsPagingUseCase
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

data class ThreadViewModelParams(
    val sourceId: String,
    val threadId: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadViewModel(
    params: ThreadViewModelParams,
    private val getTopicDetailUseCase: GetTopicDetailUseCase,
    private val getTopicCommentsPagingUseCase: GetTopicCommentsPagingUseCase,
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
    private val threadId = params.threadId

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

    val replies: Flow<PagingData<ThreadReply>> =
        loadRequest.flatMapLatest { request ->
            getTopicCommentsPagingUseCase(
                sourceId = sourceId,
                threadId = request.threadId,
                isPoOnly = request.isPoOnly,
                initialPage = request.page
            )
        }.cachedIn(screenModelScope)

    init {
        observeThreadDetails()
        updateLastAccessTime()
        _state.update { it.copy(replies = replies) }

        screenModelScope.launch {
            observeSubscriptionStatus()
            loadRequest.collect { request ->
                _state.update { it.copy(isPoOnlyMode = request.isPoOnly) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.JumpToPage -> loadRequest.update { it.copy(page = event.page) }
            Event.Refresh -> refresh()
            Event.TogglePoOnlyMode -> loadRequest.update { it.copy(isPoOnly = !it.isPoOnly) }
            Event.ToggleSubscription -> toggleSubscription()
            Event.CopyLink -> copyLink()
            is Event.UpdateLastReadReplyId -> updateLastReadReplyId(event.id)
            is Event.ShowImagePreview -> showImagePreview()
            Event.LoadMoreImages -> {}
            is Event.CopyContent -> copyContent(event.content)
            is Event.BookmarkThread -> bookmarkThread(event.thread)
            is Event.BookmarkReply -> bookmarkReply(event.reply)
            is Event.BookmarkImage -> bookmarkImage(event.image)
        }
    }

    private fun refresh() {
        // 触发 Paging 刷新的方式是让 PagingSource 失效
        // 这里我们通过重新请求第一页来间接触发 RemoteMediator 的 REFRESH
        loadRequest.update { it.copy(page = 1) }
        // 同时，也需要一个机制来强制刷新主楼信息
        observeThreadDetails(forceRefresh = true)
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
            updateTopicLastReadCommentIdUseCase(tid.toString(), replyId)
        }
    }

    private fun observeThreadDetails(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getTopicDetailUseCase(sourceId, threadId, forceRefresh)
                .flatMapLatest { detail ->
                    getChannelNameUseCase(sourceId, detail.channelId).map { forumName ->
                        detail to (forumName ?: "")
                    }
                }
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.toAppError { observeThreadDetails(forceRefresh = true) })
                    }
                }
                .collectLatest { (detail, forumName) ->
                    val thread = detail
                    val totalPages =
                        (thread.commentCount / 19) + if (thread.commentCount % 19 > 0) 1 else 0

                    _state.update {
                        it.copy(
                            isLoading = false,
                            thread = thread,
                            lastReadCommentId = detail.lastViewedCommentId,
                            totalPages = totalPages.toInt().coerceAtLeast(1),
                            forumName = forumName
                        )
                    }
                }
        }
    }

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
        val thread = state.value.thread ?: return

        _state.update { it.copy(isTogglingSubscription = true) }

        val currentSubscribed = state.value.isSubscribed
        screenModelScope.launch {
            val subscriptionKey =
                getActiveSubscriptionKeyUseCase() ?: throw IllegalStateException("未设置订阅ID")
            toggleSubscriptionUseCase(subscriptionKey, thread.id.toString(), currentSubscribed)
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

    private fun bookmarkThread(thread: Post) {
        screenModelScope.launch {
            addBookmarkUseCase(
                Bookmark.Quote(
                    id = "nmb.Thread.${thread.id}",
                    createdAt = Clock.System.now(),
                    tags = listOf(),
                    content = thread.content,
                    sourceId = thread.id,
                    sourceType = "nmb.Thread"
                )
            )
            _effect.send(Effect.ShowSnackbar("主楼已收藏"))
        }
    }

    private fun bookmarkReply(reply: ThreadReply) {
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
