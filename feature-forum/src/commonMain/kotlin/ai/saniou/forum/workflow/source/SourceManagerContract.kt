package ai.saniou.forum.workflow.source

import ai.saniou.thread.domain.model.source.SourceDescriptor

interface SourceManagerContract {
    data class State(
        val descriptors: List<SourceDescriptor> = emptyList(),
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val editing: SourceDescriptor? = null,
        val showEditor: Boolean = false,
    )

    sealed interface Event {
        data object AddDiscourse : Event
        data class Edit(val descriptor: SourceDescriptor) : Event
        data object DismissEditor : Event
        data class SaveDiscourse(
            val id: String,
            val displayName: String,
            val baseUrl: String,
            val developmentApiKey: String,
        ) : Event
        data class SetEnabled(val sourceId: String, val enabled: Boolean) : Event
        data class Remove(val sourceId: String) : Event
    }

    sealed interface Effect {
        data class Message(val value: String) : Effect
    }
}
