package ai.saniou.nmb.workflow.reference

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.domain.GetReferenceUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 引用ViewModel
 *
 * 用于获取引用的回复内容
 */
class ReferenceViewModel(
    private val referenceUseCase: GetReferenceUseCase
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
                val reply = referenceUseCase(refId)
                _uiState.emit(UiStateWrapper.Success(reply))
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

