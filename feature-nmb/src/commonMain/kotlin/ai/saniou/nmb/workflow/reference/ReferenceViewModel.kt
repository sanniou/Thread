package ai.saniou.nmb.workflow.reference

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.data.usecase.ReferenceUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 引用ViewModel
 *
 * 用于获取引用的回复内容
 */
class ReferenceViewModel(
    private val referenceUseCase: ReferenceUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<UiStateWrapper>(UiStateWrapper.Loading)

    val uiState = _uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(3000),
        UiStateWrapper.Loading
    )

    /**
     * 获取引用的回复内容
     */
    fun getReference(refId: Long) {
        viewModelScope.launch {
            try {
                _uiState.emit(UiStateWrapper.Loading)
                val reply = referenceUseCase.getReference(refId)
                if (reply != null) {
                    _uiState.emit(UiStateWrapper.Success(reply))
                } else {
                    _uiState.emit(UiStateWrapper.Error(RuntimeException("未找到引用内容"), "未找到引用内容"))
                }
            } catch (e: Throwable) {
                _uiState.emit(UiStateWrapper.Error(e, "获取引用内容失败: ${e.message}"))
            }
        }
    }

    /**
     * 清除状态
     */
    fun clear() {
        _uiState.value = UiStateWrapper.Loading
    }
}

