package ai.saniou.nmb.workflow.forum

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.ShowF
import ai.saniou.nmb.domain.ForumUserCase
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

class ForumViewModel(private val forumUserCase: ForumUserCase) : ViewModel() {

    private val dataUiState = MutableStateFlow(
        ShowForumUiState(
            showF = emptyList(),
            page = 1,
            id = 0,
            onUpdateForumId = {
                refreshForum()
            },
            onClickThread = {
                // 处理帖子点击事件
            },
            onLoadNextPage = {
                loadNextPage()
            }
        )
    )

    // 使用StateFlow管理forumId，确保只在值变化时触发加载
    private val _forumId = MutableStateFlow<Long?>(null)

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
                .filter { it > 0 }
                .distinctUntilChanged() // 确保只有在值变化时才触发
                .collect { id ->
                    loadForumInternal(id)
                }
        }
    }

    // 公开方法，设置forumId
    fun setForumId(forumId: Long?) {
        _forumId.value = forumId
    }

    // 刷新当前论坛
    fun refreshForum() {
        val currentId = _forumId.value
        if (currentId != null && currentId > 0) {
            viewModelScope.launch {
                _uiState.emit(UiStateWrapper.Loading)
                loadForumInternal(currentId)
            }
        }
    }

    // 私有方法，实际加载数据
    private suspend fun loadForumInternal(fid: Long) {
        try {
            _uiState.emit(UiStateWrapper.Loading)
            val dataList = forumUserCase(fid, 1)

            // 获取论坛名称 - 如果有数据，使用第一个帖子的fid来确定论坛名称
            var forumName = "论坛 $fid"
            if (dataList.isNotEmpty()) {
                // 这里可以从数据中获取论坛名称，或者从其他地方获取
                // 暂时使用简单的名称
                forumName = "论坛 $fid"
            }

            updateUiState { state ->
                state.copy(
                    id = fid,
                    showF = dataList,
                    page = 1,
                    forumName = forumName
                )
            }
            _uiState.emit(UiStateWrapper.Success(dataUiState.value))
        } catch (e: Throwable) {
            _uiState.emit(UiStateWrapper.Error(e, "加载论坛失败: ${e.message}"))
        }
    }


    private fun updateUiState(invoke: (ShowForumUiState) -> ShowForumUiState) {
        dataUiState.update(invoke)
    }

    // 加载下一页
    fun loadNextPage() {
        val currentId = _forumId.value
        if (currentId == null || currentId <= 0) return

        // 如果已经没有更多数据，则不再加载
        if (!dataUiState.value.hasMoreData) return

        val nextPage = dataUiState.value.page + 1

        viewModelScope.launch {
            try {
                val newDataList = forumUserCase(currentId, nextPage)

                // 合并帖子列表
                val currentList = dataUiState.value.showF
                val combinedList = currentList + newDataList

                // 判断是否还有更多数据
                // 如果返回的帖子为空，则认为没有更多数据
                val hasMoreData = newDataList.isNotEmpty()

                updateUiState { state ->
                    state.copy(
                        showF = combinedList,
                        page = nextPage,
                        hasMoreData = hasMoreData
                    )
                }
                _uiState.emit(UiStateWrapper.Success(dataUiState.value))
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
}

data class ShowForumUiState(
    var showF: List<ShowF>,
    var page: Long = 1,
    var id: Long = 0,
    var forumName: String = "",
    var hasMoreData: Boolean = true,
    var onUpdateForumId: (Long) -> Unit,
    var onClickThread: (Long) -> Unit,
    var onLoadNextPage: () -> Unit = {},
) : UiStateWrapper
