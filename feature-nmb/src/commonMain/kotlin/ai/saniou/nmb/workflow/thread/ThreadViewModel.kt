package ai.saniou.nmb.workflow.thread

import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.domain.GetThreadDetailUseCase
import ai.saniou.nmb.domain.GetThreadRepliesPagingUseCase
import ai.saniou.nmb.workflow.thread.ThreadContract.Event
import ai.saniou.nmb.workflow.thread.ThreadContract.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThreadViewModel(
    // TODO: 订阅功能也应抽象为 UseCase
    private val nmbXdApi: NmbXdApi,
    private val subscriptionStorage: SubscriptionStorage,
    private val getThreadDetailUseCase: GetThreadDetailUseCase,
    private val getThreadRepliesPagingUseCase: GetThreadRepliesPagingUseCase,
    private val forumUseCase: ForumUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var threadId: Long? = null
    private var loadJob: Job? = null

    init {
        // 加载订阅ID，为订阅功能做准备
        viewModelScope.launch {
            subscriptionStorage.loadLastSubscriptionId()
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadThread -> {
                threadId = event.id
                loadThread()
            }
            is Event.JumpToPage -> jumpToPage(event.page)
            Event.Refresh -> loadThread()
            Event.TogglePoOnlyMode -> togglePoOnlyMode()
            Event.ToggleSubscription -> toggleSubscription()
            Event.SnackbarMessageShown -> _state.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun loadThread() {
        val id = threadId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // 开始监听帖子主楼信息
            launch {
                getThreadDetailUseCase(id)
                    .catch { e ->
                        _state.update { it.copy(isLoading = false, error = "加载主楼失败: ${e.message}") }
                    }
                    .collectLatest { thread ->
                        val totalPages = (thread.replyCount / 19) + if (thread.replyCount % 19 > 0) 1 else 0
                        _state.update {
                            it.copy(
                                isLoading = false,
                                thread = thread,
                                totalPages = totalPages.toInt().coerceAtLeast(1),
                                forumName = forumUseCase.getForumName(thread.fid)
                            )
                        }
                    }
            }

            // 加载回复
            loadReplies(id, state.value.isPoOnlyMode)
        }
    }

    private fun loadReplies(threadId: Long, isPoOnly: Boolean, page: Int? = null) {
        val repliesFlow = getThreadRepliesPagingUseCase(threadId, isPoOnly, page)
        _state.update { it.copy(replies = repliesFlow, currentPage = page ?: 1) }
    }

    private fun jumpToPage(page: Int) {
        val id = threadId ?: return
        loadReplies(id, state.value.isPoOnlyMode, page)
    }

    private fun togglePoOnlyMode() {
        val id = threadId ?: return
        val newPoOnlyMode = !state.value.isPoOnlyMode
        _state.update { it.copy(isPoOnlyMode = newPoOnlyMode) }
        loadReplies(id, newPoOnlyMode)
    }

    private fun toggleSubscription() {
        val id = threadId ?: return
        val currentSubscribed = state.value.isSubscribed
        val newSubscribed = !currentSubscribed

        viewModelScope.launch {
            try {
                val subscriptionId = subscriptionStorage.subscriptionId.value
                    ?: throw IllegalStateException("订阅ID未加载")

                val resultMessage = if (newSubscribed) {
                    nmbXdApi.addFeed(subscriptionId, id)
                } else {
                    nmbXdApi.delFeed(subscriptionId, id)
                }

                _state.update {
                    it.copy(
                        isSubscribed = newSubscribed,
                        snackbarMessage = resultMessage
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubscribed = currentSubscribed, // 恢复原状
                        snackbarMessage = "操作失败: ${e.message}"
                    )
                }
            }
        }
    }
}
