package ai.saniou.nmb.workflow.thread

import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.domain.AddBookmarkUseCase
import ai.saniou.nmb.domain.GetThreadDetailUseCase
import ai.saniou.nmb.domain.GetThreadRepliesPagingUseCase
import ai.saniou.nmb.domain.ToggleSubscriptionUseCase
import ai.saniou.nmb.workflow.thread.ThreadContract.Effect
import ai.saniou.nmb.workflow.thread.ThreadContract.Event
import ai.saniou.nmb.workflow.thread.ThreadContract.State
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadViewModel(
    private val threadId: Long,
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val getThreadRepliesPagingUseCase: GetThreadRepliesPagingUseCase,
    private val nmbRepository: NmbRepository,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val subscriptionStorage: SubscriptionStorage,
    private val db: Database,
) : ScreenModel {

    private data class LoadRequest(
        val threadId: Long,
        val isPoOnly: Boolean = false,
        val page: Int = 1
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadRequest = MutableStateFlow(LoadRequest(threadId = threadId))

    val replies: Flow<PagingData<ThreadReply>> =
        loadRequest.flatMapLatest { request ->
            getThreadRepliesPagingUseCase(
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
            subscriptionStorage.loadLastSubscriptionId()
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
        screenModelScope.launch {
            nmbRepository.updateThreadLastAccessTime(threadId, Clock.System.now().epochSeconds)
        }
    }

    private fun updateLastReadReplyId(replyId: Long) {
        screenModelScope.launch {
            nmbRepository.updateThreadLastReadReplyId(threadId, replyId)
        }
    }

    private fun observeThreadDetails(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getThreadDetailUseCase(threadId, forceRefresh)
                .catch { e ->
                    _state.update {
                        it.copy(isLoading = false, error = "加载主楼失败: ${e.message}")
                    }
                }
                .collectLatest { detail ->
                    val thread = detail.thread
                    val totalPages = (thread.replyCount / 19) + if (thread.replyCount % 19 > 0) 1 else 0
                    _state.update {
                        it.copy(
                            isLoading = false,
                            thread = thread,
                            lastReadReplyId = detail.lastReadReplyId,
                            totalPages = totalPages.toInt().coerceAtLeast(1),
                            forumName = db.forumQueries.getForum(thread.fid).executeAsOneOrNull()?.name ?: ""
                        )
                    }
                }
        }
    }

    private fun observeSubscriptionStatus() {
        val subscriptionKey = subscriptionStorage.subscriptionId.value ?: throw IllegalStateException("未设置订阅ID")
        screenModelScope.launch {
            nmbRepository.observeIsSubscribed(subscriptionKey, threadId).collect { isSubscribed ->
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
            toggleSubscriptionUseCase(thread, currentSubscribed)
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

    private fun bookmarkThread(thread: Thread) {
        screenModelScope.launch {
            addBookmarkUseCase(thread.id.toString(), thread.content)
            _effect.send(Effect.ShowSnackbar("主楼已收藏"))
        }
    }

    private fun bookmarkReply(reply: ThreadReply) {
        screenModelScope.launch {
            addBookmarkUseCase(reply.id.toString(), reply.content)
            _effect.send(Effect.ShowSnackbar("回复已收藏"))
        }
    }

    private fun showImagePreview() {
        screenModelScope.launch {
            _effect.send(Effect.NavigateToImagePreview)
        }
    }
}
