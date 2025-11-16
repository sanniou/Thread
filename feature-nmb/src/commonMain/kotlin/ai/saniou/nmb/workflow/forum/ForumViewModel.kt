package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.Forum
import ai.saniou.nmb.domain.ForumUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumViewModel(private val forumUserCase: ForumUseCase) : ViewModel() {

    // 使用StateFlow管理forumId，确保只在值变化时触发加载
    private val _forumId = MutableStateFlow<Pair<Long, Long>?>(null)

    // UI状态
    private val _uiState = MutableStateFlow<UiStateWrapper>(UiStateWrapper.Loading)

    val uiState = _uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(3000),
        UiStateWrapper.Loading
    )

    init {
        // 监听forumId变化并加载数据
        viewModelScope.launch {
            _forumId
                .filterNotNull()
                .distinctUntilChanged() // 确保只有在值变化时才触发
                .collect { (fgroup, id) ->
                    loadForumInternal(id, fgroup)
                }
        }
    }

    // 公开方法，设置forumId
    fun setForumId(forumId: Long, fgroupId: Long) {
        _forumId.value = fgroupId to forumId
    }

    // 刷新当前论坛
    fun refreshForum() {
        val currentId = _forumId.value
        if (currentId != null) {
            viewModelScope.launch {
                // 直接调用loadForumInternal会发出Loading状态
                loadForumInternal(currentId.second, currentId.first)
            }
        }
    }

    // 私有方法，实际加载数据
    private suspend fun loadForumInternal(fid: Long, fgroup: Long) {
        _uiState.emit(UiStateWrapper.Loading)
        try {
            val dataList = forumUserCase(fid, fgroup).cachedIn(viewModelScope)
            val forumName = forumUserCase.getForumName(fid)
            _uiState.emit(
                UiStateWrapper.Success(
                    ShowForumUiState(
                        forum = dataList,
                        id = fid,
                        forumName = forumName,
                        onClickThread = {
                            // 处理帖子点击事件
                        },
                    )
                )
            )
        } catch (e: Throwable) {
            _uiState.emit(UiStateWrapper.Error(e, "加载论坛失败: ${e.message}"))
        }
    }
}

data class ShowForumUiState(
    var forum: Flow<PagingData<Forum>>,
    var page: Long = 1,
    var id: Long = 0,
    var forumName: String = "",
    var hasMoreData: Boolean = true,
    var onClickThread: (Long) -> Unit,
) : UiStateWrapper
