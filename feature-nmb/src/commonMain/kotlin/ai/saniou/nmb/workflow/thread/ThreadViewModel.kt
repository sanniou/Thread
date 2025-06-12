package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.ThreadReply
import ai.saniou.nmb.domain.ThreadDetailUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThreadViewModel(
    private val nmbXdApi: NmbXdApi,
    private val threadDetailUseCase: ThreadDetailUseCase,
    private val subscriptionStorage: SubscriptionStorage,
) : ViewModel() {

    // 默认空状态
    private val emptyThread = Thread(
        id = 0,
        fid = 0,
        replyCount = 0,
        img = "",
        ext = "",
        now = "",
        userHash = "",
        name = "",
        title = "",
        content = "",
        sage = 0,
        admin = 0,
        hide = 0,
        replies = emptyList(),
    )

    // 是否只显示PO的帖子
    private val _isPoOnlyMode = MutableStateFlow(false)

    // 是否已订阅
    private val _isSubscribed = MutableStateFlow(false)

    // 论坛名称
    private val _forumName = MutableStateFlow("")

    private val dataUiState = MutableStateFlow(
        ThreadUiState(
            thread = emptyThread,
            threadReplies = emptyFlow(),
            currentPage = 1,
            totalPages = 1,
            onJumpToPage = { page ->
                jumpToPage(page)
            },
            onTogglePoOnly = {
                togglePoOnlyMode()
            },
            onToggleSubscribe = { ->
                toggleSubscribe()
            }
        )
    )

    // 使用StateFlow管理threadId，确保只在值变化时触发加载
    private val _threadId = MutableStateFlow<Long?>(null)

    // UI状态
    private val _uiState = MutableStateFlow<UiStateWrapper>(UiStateWrapper.Loading)

    val uiState = _uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(3000),
        UiStateWrapper.Loading
    )

    init {
        // 监听threadId变化并加载数据
        viewModelScope.launch {
            _threadId
                .filterNotNull()
                .filter { it > 0 }
                .distinctUntilChanged() // 确保只有在值变化时才触发
                .collect { id ->
                    loadThreadInternal(id)
                    // 加载订阅状态
                    checkSubscriptionStatus(id)
                }
        }

        // 加载订阅ID
        viewModelScope.launch {
            subscriptionStorage.loadLastSubscriptionId()
        }
    }

    /**
     * 检查当前帖子的订阅状态
     */
    private suspend fun checkSubscriptionStatus(threadId: Long) {
        // 目前API没有提供检查订阅状态的方法，这里暂时不实现
        // 未来可以通过获取订阅列表并检查是否包含当前帖子来实现
    }

    // 公开方法，设置threadId
    fun setThreadId(threadId: Long?) {
        _threadId.value = threadId
    }

    // 私有方法，实际加载数据
    private suspend fun loadThreadInternal(threadId: Long) {
        try {
            _uiState.emit(UiStateWrapper.Loading)

            val thread = threadDetailUseCase.getThread(threadId)
            // 获取论坛名称
            getForumName(thread.fid)

            // 设置总页数，根据回复数量计算，每页显示19条回复
            val totalReplies = thread.replyCount
            val repliesPerPage = 20 // 每页显示的回复数量
            val totalPages =
                (totalReplies / repliesPerPage) + if (totalReplies % repliesPerPage > 0) 1 else 0

            val threadReplies = threadDetailUseCase(threadId, _isPoOnlyMode.value)
            updateUiState { state ->
                state.copy(
                    thread = thread,
                    threadReplies = threadReplies,
                    currentPage = 1,
                    totalPages = totalPages.toInt().coerceAtLeast(1),
                    forumName = _forumName.value,
                    isPoOnlyMode = _isPoOnlyMode.value,
                    isSubscribed = _isSubscribed.value
                )
            }
            _uiState.emit(UiStateWrapper.Success(dataUiState.value))
        } catch (e: Throwable) {
            _uiState.emit(UiStateWrapper.Error(e, "加载帖子失败: ${e.message}"))
        }
    }


    fun onReplyClicked(replyId: Long) {
        // 处理回复点击事件，可以实现引用回复等功能
    }

    /**
     * 跳转到指定页面
     */
    private fun jumpToPage(page: Int) {
        TODO()
    }

    /**
     * 切换只看PO模式
     */
    private fun togglePoOnlyMode() {
        _isPoOnlyMode.value = !_isPoOnlyMode.value
        _threadId.value?.run {
            val threadReplies = threadDetailUseCase(this, _isPoOnlyMode.value)
            updateUiState { state ->
                state.copy(
                    threadReplies = threadReplies,
                    isPoOnlyMode = _isPoOnlyMode.value
                )
            }
        }
    }

    /**
     * 切换订阅状态
     */
    private fun toggleSubscribe() {
        val subscribed = !_isSubscribed.value
        val currentId = _threadId.value
        if (currentId == null || currentId <= 0) return

        viewModelScope.launch {
            try {
                val subscriptionId = subscriptionStorage.subscriptionId.value

                if (subscriptionId == null) {
                    // 如果没有订阅ID，show error
                    throw Exception("没有订阅ID")
                }
                val resultMessage =
                    // 使用已有ID进行订阅/取消订阅操作
                    if (subscribed) {
                        nmbXdApi.addFeed(subscriptionId, currentId)
                    } else {
                        nmbXdApi.delFeed(subscriptionId, currentId)
                    }


                // 更新订阅状态
                _isSubscribed.value = subscribed

                // 更新UI状态
                updateUiState { state ->
                    state.copy(isSubscribed = subscribed, subscribedMessage = resultMessage)
                }

                // 更新UI
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
            } catch (e: Exception) {
                // 订阅/取消订阅失败，恢复状态
                _isSubscribed.value = !subscribed
                updateUiState { state ->
                    state.copy(isSubscribed = !subscribed, subscribedMessage = e.message)
                }
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
            }
        }
    }

    /**
     * 获取论坛名称
     */
    private fun getForumName(fid: Long) {
        // TODO: 实现获取论坛名称的逻辑
        // 暂时使用简单的名称
        _forumName.value = "论坛 $fid"

        // 更新UI状态
        updateUiState { state ->
            state.copy(forumName = _forumName.value)
        }
    }

    private fun updateUiState(invoke: (ThreadUiState) -> ThreadUiState) {
        dataUiState.update(invoke)
    }
}

data class ThreadUiState(
    val thread: Thread,
    val threadReplies: Flow<PagingData<ThreadReply>>,
    val currentPage: Int,
    val totalPages: Int,
    val hasMoreData: Boolean = true,
    val isPoOnlyMode: Boolean = false,
    val isSubscribed: Boolean = false,
    val subscribedMessage: String? = null,
    val forumName: String = "",
    val onJumpToPage: (Int) -> Unit = {},
    val onTogglePoOnly: () -> Unit = {},
    val onToggleSubscribe: () -> Unit = {},
) : UiStateWrapper
