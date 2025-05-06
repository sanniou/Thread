package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.domain.ThreadUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThreadViewModel(
    private val threadUseCase: ThreadUseCase,
    private val nmbXdApi: NmbXdApi,
    private val subscriptionStorage: SubscriptionStorage
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
        replies = emptyList()
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
            currentPage = 1,
            totalPages = 1,
            onRefresh = {
                refreshThread()
            },
            onLoadNextPage = {
                loadNextPage()
            },
            onJumpToPage = { page ->
                jumpToPage(page)
            },
            onTogglePoOnly = {
                togglePoOnlyMode()
            },
            onToggleSubscribe = { subscribed ->
                toggleSubscribe(subscribed)
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
            subscriptionStorage.getSubscriptionId()
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
            val thread = threadUseCase(threadId, 1)

            // 获取论坛名称
            getForumName(thread.fid)

            // 设置总页数，根据回复数量计算，每页显示19条回复
            val totalReplies = thread.replyCount
            val repliesPerPage = 19 // 每页显示的回复数量
            val totalPages = (totalReplies / repliesPerPage) + if (totalReplies % repliesPerPage > 0) 1 else 0

            updateUiState { state ->
                state.copy(
                    thread = thread,
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

    // 刷新当前帖子
    private fun refreshThread() {
        val currentId = _threadId.value
        if (currentId != null && currentId > 0) {
            viewModelScope.launch {
                if (_isPoOnlyMode.value) {
                    // 如果是只看PO模式，则使用getThreadPo方法
                    try {
                        _uiState.emit(UiStateWrapper.Loading)
                        val poThreads = threadUseCase.getThreadPo(currentId, 1)
                            updateUiState { state ->
                                state.copy(
                                    thread = poThreads,
                                    currentPage = 1,
                                    isPoOnlyMode = true
                                )
                            }
                            _uiState.emit(UiStateWrapper.Success(dataUiState.value))
                    } catch (e: Throwable) {
                        _uiState.emit(UiStateWrapper.Error(e, "刷新帖子失败: ${e.message}"))
                    }
                } else {
                    // 如果不是只看PO模式，则使用正常的加载方法
                    loadThreadInternal(currentId)
                }
            }
        }
    }

    // 加载下一页回复
    private fun loadNextPage() {
        val currentId = _threadId.value
        if (currentId == null || currentId <= 0) return

        // 如果已经没有更多数据，则不再加载
        if (!dataUiState.value.hasMoreData) return

        val nextPage = dataUiState.value.currentPage + 1

        viewModelScope.launch {
            try {
                if (_isPoOnlyMode.value) {
                    // 如果是只看PO模式，则使用getThreadPo方法加载下一页
                    val poThreads = threadUseCase.getThreadPo(currentId, nextPage.toLong())
                        // 合并回复列表
                        val currentThread = dataUiState.value.thread
                        val newReplies = currentThread.replies + poThreads.replies

                        // 判断是否还有更多数据
                        val hasMoreData = poThreads.replies.isNotEmpty()

                        updateUiState { state ->
                            state.copy(
                                thread = currentThread.copy(replies = newReplies),
                                currentPage = nextPage,
                                hasMoreData = hasMoreData
                            )
                        }
                        _uiState.emit(UiStateWrapper.Success(dataUiState.value))
                } else {
                    // 如果不是只看PO模式，则使用常规API加载下一页
                    val threadData = threadUseCase(currentId, nextPage.toLong())

                    // 合并回复列表
                    val currentThread = dataUiState.value.thread
                    val newReplies = currentThread.replies + threadData.replies

                    // 判断是否还有更多数据
                    // 如果返回的回复为空，则认为没有更多数据
                    val hasMoreData = threadData.replies.isNotEmpty()

                    updateUiState { state ->
                        state.copy(
                            thread = currentThread.copy(replies = newReplies),
                            currentPage = nextPage,
                            hasMoreData = hasMoreData
                        )
                    }
                    _uiState.emit(UiStateWrapper.Success(dataUiState.value))
                }
            } catch (e: Throwable) {
                // 加载下一页失败，但不影响当前页面显示
                // 将hasMoreData设置为false，避免继续尝试加载
                updateUiState { state ->
                    state.copy(hasMoreData = false)
                }
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
            }
        }
    }

    fun onReplyClicked(replyId: Long) {
        // 处理回复点击事件，可以实现引用回复等功能
    }

    fun navigateToReply(threadId: Long) {
        // 导航到回复页面
    }

    /**
     * 跳转到指定页面
     */
    private fun jumpToPage(page: Int) {
        val currentId = _threadId.value
        if (currentId == null || currentId <= 0) return
        if (page <= 0 || page > dataUiState.value.totalPages) return

        viewModelScope.launch {
            try {
                _uiState.emit(UiStateWrapper.Loading)
                val threadData = threadUseCase(currentId, page.toLong())

                updateUiState { state ->
                    state.copy(
                        thread = threadData,
                        currentPage = page
                    )
                }
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
            } catch (e: Throwable) {
                _uiState.emit(UiStateWrapper.Error(e, "加载页面失败: ${e.message}"))
            }
        }
    }

    /**
     * 切换只看PO模式
     */
    private fun togglePoOnlyMode() {
        val currentId = _threadId.value
        if (currentId == null || currentId <= 0) return

        val newPoOnlyMode = !_isPoOnlyMode.value
        _isPoOnlyMode.value = newPoOnlyMode

        viewModelScope.launch {
            try {
                _uiState.emit(UiStateWrapper.Loading)

                if (newPoOnlyMode) {
                    // 切换到只看PO模式，使用getThreadPo方法
                    val poThreads = threadUseCase.getThreadPo(currentId, 1)
                        updateUiState { state ->
                            state.copy(
                                thread = poThreads,
                                currentPage = 1,
                                isPoOnlyMode = true
                            )
                        }
                        _uiState.emit(UiStateWrapper.Success(dataUiState.value))
                } else {
                    // 切换到正常模式，使用常规API加载
                    val threadData = threadUseCase(currentId, 1)
                    updateUiState { state ->
                        state.copy(
                            thread = threadData,
                            currentPage = 1,
                            isPoOnlyMode = false
                        )
                    }
                    _uiState.emit(UiStateWrapper.Success(dataUiState.value))
                }
            } catch (e: Throwable) {
                _isPoOnlyMode.value = !newPoOnlyMode // 切换失败，恢复状态
                _uiState.emit(UiStateWrapper.Error(e, "切换模式失败: ${e.message}"))
            }
        }
    }

    /**
     * 切换订阅状态
     */
    private fun toggleSubscribe(subscribed: Boolean) {
        val currentId = _threadId.value
        if (currentId == null || currentId <= 0) return

        viewModelScope.launch {
            try {
                val subscriptionId = subscriptionStorage.getSubscriptionId()

                if (subscriptionId == null) {
                    // 如果没有订阅ID，生成一个随机ID并保存
                    val newId = subscriptionStorage.generateRandomSubscriptionId()
                    subscriptionStorage.saveSubscriptionId(newId)

                    // 使用新ID进行订阅/取消订阅操作
                    if (subscribed) {
                        nmbXdApi.addFeed(newId, currentId)
                    } else {
                        nmbXdApi.delFeed(newId, currentId)
                    }
                } else {
                    // 使用已有ID进行订阅/取消订阅操作
                    if (subscribed) {
                        nmbXdApi.addFeed(subscriptionId, currentId)
                    } else {
                        nmbXdApi.delFeed(subscriptionId, currentId)
                    }
                }

                // 更新订阅状态
                _isSubscribed.value = subscribed

                // 更新UI状态
                updateUiState { state ->
                    state.copy(isSubscribed = subscribed)
                }

                // 更新UI
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
            } catch (e: Exception) {
                // 订阅/取消订阅失败，恢复状态
                _isSubscribed.value = !subscribed
                updateUiState { state ->
                    state.copy(isSubscribed = !subscribed)
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
    val currentPage: Int,
    val totalPages: Int,
    val hasMoreData: Boolean = true,
    val isPoOnlyMode: Boolean = false,
    val isSubscribed: Boolean = false,
    val forumName: String = "",
    val onRefresh: () -> Unit,
    val onLoadNextPage: () -> Unit,
    val onJumpToPage: (Int) -> Unit = {},
    val onTogglePoOnly: () -> Unit = {},
    val onToggleSubscribe: (Boolean) -> Unit = {}
) : UiStateWrapper
