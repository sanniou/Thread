package ai.saniou.forum.workflow.init

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.init.SourceInitContract.Event
import ai.saniou.forum.workflow.init.SourceInitContract.State
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SubscriptionRepository
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.domain.repository.SourceRepository
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceInitViewModel(
    private val sourceId: String,
    sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : ScreenModel {

    private val source = sourceRepository.getSource(sourceId)
    private val _state = MutableStateFlow(State(sourceName = source?.name ?: sourceId))
    val state = _state.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            Event.CompleteInitialization -> completeInitialization()
        }
    }

    private fun completeInitialization() {
        screenModelScope.launch {
            _state.update { it.copy(uiState = UiStateWrapper.Loading) }
            try {
                if (subscriptionRepository.getActiveSubscriptionKey() == null) {
                    subscriptionRepository.addSubscriptionKey(
                        subscriptionRepository.generateRandomSubscriptionId()
                    )
                }
                settingsRepository.saveValue("${sourceId}_initialized", true)
                _state.update { it.copy(isInitialized = true, uiState = UiStateWrapper.Success(Unit)) }
            } catch (e: Exception) {
                _state.update { it.copy(uiState = UiStateWrapper.Error(e.toAppError())) }
            }
        }
    }
}
