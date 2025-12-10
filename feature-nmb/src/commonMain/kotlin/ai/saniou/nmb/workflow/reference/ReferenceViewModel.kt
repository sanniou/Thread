package ai.saniou.nmb.workflow.reference

import ai.saniou.nmb.workflow.reference.ReferenceContract.Event
import ai.saniou.nmb.workflow.reference.ReferenceContract.State
import ai.saniou.thread.domain.usecase.post.GetReferenceUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

class ReferenceViewModel(
    private val getReferenceUseCase: GetReferenceUseCase,
) : ScreenModel {
    private val _uiState = MutableStateFlow(State())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            is Event.GetReference -> getReference(event.refId)
            Event.Clear -> clear()
        }
    }

    private fun getReference(refId: Long) {
        getReferenceUseCase(refId)
            .onStart {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            .onEach { post ->
                _uiState.update {
                    it.copy(isLoading = false, reply = post)
                }
            }
            .catch { error ->
                _uiState.update {
                    it.copy(isLoading = false, error = "获取引用内容失败: ${error.message}")
                }
            }
            .launchIn(screenModelScope)
    }

    private fun clear() {
        _uiState.update { State() }
    }
}
