package ai.saniou.forum.workflow.source

import ai.saniou.forum.workflow.source.SourceManagerContract.Effect
import ai.saniou.forum.workflow.source.SourceManagerContract.Event
import ai.saniou.forum.workflow.source.SourceManagerContract.State
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import ai.saniou.thread.domain.usecase.source.ObserveSourceDescriptorsUseCase
import ai.saniou.thread.domain.usecase.source.RemoveSourceDescriptorUseCase
import ai.saniou.thread.domain.usecase.source.SetSourceEnabledUseCase
import ai.saniou.thread.domain.usecase.source.UpsertSourceDescriptorUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceManagerViewModel(
    observeDescriptors: ObserveSourceDescriptorsUseCase,
    private val upsertDescriptor: UpsertSourceDescriptorUseCase,
    private val setSourceEnabled: SetSourceEnabledUseCase,
    private val removeDescriptor: RemoveSourceDescriptorUseCase,
) : ScreenModel {
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val effects = Channel<Effect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        screenModelScope.launch {
            observeDescriptors().collect { descriptors ->
                mutableState.update { it.copy(descriptors = descriptors, isLoading = false) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.AddDiscourse -> mutableState.update { it.copy(editing = null, showEditor = true) }
            is Event.Edit -> mutableState.update { it.copy(editing = event.descriptor, showEditor = true) }
            Event.DismissEditor -> mutableState.update { it.copy(editing = null, showEditor = false) }
            is Event.SaveDiscourse -> save(event)
            is Event.SetEnabled -> mutate { setSourceEnabled(event.sourceId, event.enabled) }
            is Event.Remove -> mutate(successMessage = "来源已删除") { removeDescriptor(event.sourceId) }
        }
    }

    private fun save(event: Event.SaveDiscourse) = mutate(successMessage = "来源已保存") {
        val id = event.id.trim().lowercase()
        val name = event.displayName.trim()
        val url = event.baseUrl.trim().let { if (it.endsWith('/')) it else "$it/" }
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Base URL 必须以 http:// 或 https:// 开头"
        }
        val existing = mutableState.value.editing
        upsertDescriptor(
            SourceDescriptor(
                id = id,
                type = SourceType.DISCOURSE,
                displayName = name,
                baseUrl = url,
                enabled = existing?.enabled ?: true,
                options = mapOf("developmentApiKey" to event.developmentApiKey.trim())
                    .filterValues(String::isNotBlank),
            )
        )
        mutableState.update { it.copy(editing = null, showEditor = false) }
    }

    private fun mutate(successMessage: String? = null, action: suspend () -> Unit) {
        if (mutableState.value.isSaving) return
        screenModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            runCatching { action() }
                .onSuccess { successMessage?.let { effects.send(Effect.Message(it)) } }
                .onFailure { effects.send(Effect.Message(it.message ?: "来源操作失败")) }
            mutableState.update { it.copy(isSaving = false) }
        }
    }
}
