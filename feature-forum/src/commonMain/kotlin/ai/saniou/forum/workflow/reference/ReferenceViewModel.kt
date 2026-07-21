package ai.saniou.forum.workflow.reference

import ai.saniou.forum.workflow.reference.ReferenceContract.Event
import ai.saniou.forum.workflow.reference.ReferenceContract.State
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
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_03e6f0a74a

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
                    it.copy(isLoading = false, error = getString(Res.string.s_03e6f0a74a, error.message.orEmpty()))
                }
            }
            .launchIn(screenModelScope)
    }

    private fun clear() {
        _uiState.update { State() }
    }
}
