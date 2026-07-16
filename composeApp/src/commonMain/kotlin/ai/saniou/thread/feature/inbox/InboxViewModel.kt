package ai.saniou.thread.feature.inbox

import ai.saniou.thread.domain.model.inbox.InboxFilter
import ai.saniou.thread.domain.repository.InboxRepository
import ai.saniou.thread.feature.inbox.InboxContract.Event
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val repository: InboxRepository,
) : ScreenModel {
    private val mutableState = MutableStateFlow(InboxContract.State())
    val state = mutableState.asStateFlow()
    private val filters = MutableStateFlow(InboxFilter())
    val events = filters.flatMapLatest(repository::getInbox).cachedIn(screenModelScope)

    init {
        screenModelScope.launch {
            repository.observeSummary().collect { summary ->
                mutableState.update { it.copy(summary = summary) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.QueryChanged -> updateFilter { copy(query = event.value.take(160)) }
            is Event.UnreadOnlyChanged -> updateFilter { copy(unreadOnly = event.value) }
            is Event.IncludeMutedChanged -> updateFilter { copy(includeMuted = event.value) }
            is Event.SourceChanged -> updateFilter { copy(sourceId = event.sourceId) }
            is Event.KindChanged -> updateFilter { copy(kind = event.kind) }
            is Event.MarkRead -> launchMutation { repository.markRead(event.id, event.read) }
            Event.MarkAllRead -> launchMutation("当前视图已全部标为已读") {
                repository.markAllRead(filters.value)
            }
            is Event.SetSourceMuted -> launchMutation(
                if (event.muted) "已静音 ${event.sourceId}" else "已恢复 ${event.sourceId}",
            ) { repository.setSourceMuted(event.sourceId, event.muted) }
            is Event.Delete -> launchMutation { repository.delete(event.id) }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun updateFilter(transform: InboxFilter.() -> InboxFilter) {
        filters.update(transform)
        mutableState.update { it.copy(filter = filters.value) }
    }

    private fun launchMutation(success: String? = null, block: suspend () -> Unit) {
        screenModelScope.launch {
            runCatching { block() }.fold(
                onSuccess = { success?.let { message -> mutableState.update { it.copy(message = message) } } },
                onFailure = { error -> mutableState.update { it.copy(message = error.message ?: "操作失败") } },
            )
        }
    }
}
