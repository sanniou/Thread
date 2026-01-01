package ai.saniou.forum.workflow.init

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.forum.workflow.init.SourceInitContract.Event
import ai.saniou.forum.workflow.init.SourceInitContract.State
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SubscriptionRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SourceInitViewModel(
    private val sourceId: String,
    private val settingsRepository: SettingsRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val nmbSource: NmbSource,
) : ScreenModel {

    private val _state = MutableStateFlow(State(sourceName = getSourceName(sourceId)))
    val state = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun getSourceName(id: String): String {
        return when (id) {
            "nmb" -> "A岛"
            "discourse" -> "Discourse"
            "nga" -> "NGA"
            else -> id
        }
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            when (sourceId) {
                "nmb" -> {
                    val key = subscriptionRepository.getActiveSubscriptionKey()
                    val accounts = nmbSource.getSortedAccounts()
                    val firstCookie = accounts.firstOrNull()?.value ?: ""
                    _state.update {
                        it.copy(
                            nmbSubscriptionKey = key ?: "",
                            nmbCookie = firstCookie
                        )
                    }
                }
                "discourse" -> {
                    val apiKey = settingsRepository.getValue<String>("discourse_api_key")
                    val username = settingsRepository.getValue<String>("discourse_username")
                    _state.update {
                        it.copy(
                            discourseApiKey = apiKey ?: "",
                            discourseUsername = username ?: ""
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onEvent(event: Event) {
        when (event) {
            is Event.UpdateNmbSubscriptionKey -> {
                _state.update { it.copy(nmbSubscriptionKey = event.key) }
            }
            is Event.UpdateNmbCookie -> {
                _state.update { it.copy(nmbCookie = event.cookie) }
            }
            is Event.UpdateDiscourseApiKey -> {
                _state.update { it.copy(discourseApiKey = event.key) }
            }
            is Event.UpdateDiscourseUsername -> {
                _state.update { it.copy(discourseUsername = event.username) }
            }
            Event.CompleteInitialization -> completeInitialization()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun completeInitialization() {
        screenModelScope.launch {
            _state.update { it.copy(uiState = UiStateWrapper.Loading) }
            try {
                when (sourceId) {
                    "nmb" -> {
                        val key = state.value.nmbSubscriptionKey.ifBlank {
                            Uuid.random().toString()
                        }
                        subscriptionRepository.addSubscriptionKey(key)

                        if (state.value.nmbCookie.isNotBlank()) {
                            nmbSource.insertAccount("Default", state.value.nmbCookie)
                        }

                        settingsRepository.saveValue("nmb_initialized", true)
                    }
                    "discourse" -> {
                        settingsRepository.saveValue("discourse_api_key", state.value.discourseApiKey)
                        settingsRepository.saveValue("discourse_username", state.value.discourseUsername)
                        settingsRepository.saveValue("discourse_initialized", true)
                    }
                    else -> {
                        // Generic initialization
                    }
                }
                _state.update { it.copy(isInitialized = true, uiState = UiStateWrapper.Success(Unit)) }
            } catch (e: Exception) {
                // Handle error
                _state.update { it.copy(uiState = UiStateWrapper.Success(Unit)) } // Fallback to success for now or show error
            }
        }
    }
}
