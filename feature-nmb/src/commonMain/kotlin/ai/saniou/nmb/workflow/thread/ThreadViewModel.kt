package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.Thread
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

class ThreadViewModel(private val threadUseCase: ThreadUseCase) : ViewModel() {

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
                }
        }
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
            if (thread.isNotEmpty()) {
                updateUiState { state ->
                    state.copy(
                        thread = thread[0],
                        currentPage = 1
                    )
                }
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
            } else {
                _uiState.emit(UiStateWrapper.Error(IllegalStateException("帖子不存在"), "帖子不存在或已被删除"))
            }
        } catch (e: Throwable) {
            _uiState.emit(UiStateWrapper.Error(e, "加载帖子失败: ${e.message}"))
        }
    }

    // 刷新当前帖子
    private fun refreshThread() {
        val currentId = _threadId.value
        if (currentId != null && currentId > 0) {
            viewModelScope.launch {
                loadThreadInternal(currentId)
            }
        }
    }

    // 加载下一页回复
    private fun loadNextPage() {
        val currentId = _threadId.value
        if (currentId == null || currentId <= 0) return

        val nextPage = dataUiState.value.currentPage + 1

        viewModelScope.launch {
            try {
                val threadData = threadUseCase(currentId, nextPage.toLong())

                if (threadData.isNotEmpty()) {
                    // 合并回复列表
                    val currentThread = dataUiState.value.thread
                    val newReplies = currentThread.replies + threadData[0].replies

                    updateUiState { state ->
                        state.copy(
                            thread = currentThread.copy(replies = newReplies),
                            currentPage = nextPage
                        )
                    }
                    _uiState.emit(UiStateWrapper.Success(dataUiState.value))
                }
            } catch (e: Throwable) {
                // 加载下一页失败，但不影响当前页面显示
                // 可以显示一个提示
            }
        }
    }

    fun onReplyClicked(replyId: Long) {
        // 处理回复点击事件，可以实现引用回复等功能
    }

    fun navigateToReply(threadId: Long) {
        // 导航到回复页面
    }

    private fun updateUiState(invoke: (ThreadUiState) -> ThreadUiState) {
        dataUiState.update(invoke)
    }
}

data class ThreadUiState(
    val thread: Thread,
    val currentPage: Int,
    val totalPages: Int,
    val onRefresh: () -> Unit,
    val onLoadNextPage: () -> Unit
) : UiStateWrapper
