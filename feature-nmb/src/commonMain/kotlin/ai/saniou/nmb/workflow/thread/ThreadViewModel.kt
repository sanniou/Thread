package ai.saniou.nmb.workflow.thread

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.domain.ThreadUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThreadViewModel(private val threadUseCase: ThreadUseCase) : ViewModel() {
    
    private val dataUiState = MutableStateFlow(
        ThreadUiState(
            thread = Thread(
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
            ),
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
    
    private var currentThreadId: Long = 0
    
    private val _uiState = MutableStateFlow<UiStateWrapper>(UiStateWrapper.Loading)
    
    val uiState = _uiState.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(3000), 
        UiStateWrapper.Loading
    )
    
    fun loadThread(threadId: Long) {
        currentThreadId = threadId
        refreshThread()
    }
    
    private fun refreshThread() {
        if (currentThreadId <= 0) return
        
        viewModelScope.launch {
            try {
                _uiState.emit(UiStateWrapper.Loading)
                val thread = threadUseCase(currentThreadId, 1)
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
    }
    
    private fun loadNextPage() {
        if (currentThreadId <= 0) return
        
        val nextPage = dataUiState.value.currentPage + 1
        
        viewModelScope.launch {
            try {
                val thread = threadUseCase(currentThreadId, nextPage.toLong())
                if (thread.isNotEmpty()) {
                    // 合并回复列表
                    val currentThread = dataUiState.value.thread
                    val newReplies = currentThread.replies + thread[0].replies
                    
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
