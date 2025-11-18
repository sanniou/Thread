package ai.saniou.nmb.workflow.reference

import ai.saniou.nmb.domain.GetReferenceUseCase
import ai.saniou.nmb.workflow.reference.ReferenceContract.Event
import ai.saniou.nmb.workflow.reference.ReferenceContract.State
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReferenceViewModel(
    private val getReferenceUseCase: GetReferenceUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : ViewModel() {
    private val _uiState = MutableStateFlow(State())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            is Event.GetReference -> getReference(event.refId)
            Event.Clear -> clear()
        }
    }

    private fun getReference(refId: Long) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getReferenceUseCase(refId)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(isLoading = false, reply = reply)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "获取引用内容失败: ${error.message}")
                    }
                }
        }
    }

    private fun clear() {
        _uiState.update { State() }
    }
}

