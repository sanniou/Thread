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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.error_action_failed
import thread.composeapp.generated.resources.s_383bf53efb
import thread.composeapp.generated.resources.s_e31cb76a01
import thread.composeapp.generated.resources.s_fb8a49be23

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
            Event.MarkAllRead -> launchMutation(successRes = Res.string.s_fb8a49be23) {
                repository.markAllRead(filters.value)
            }
            is Event.SetSourceMuted -> launchMutation(
                successRes = if (event.muted) Res.string.s_383bf53efb else Res.string.s_e31cb76a01,
                successArgs = listOf(event.sourceId),
            ) { repository.setSourceMuted(event.sourceId, event.muted) }
            is Event.Delete -> launchMutation { repository.delete(event.id) }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun updateFilter(transform: InboxFilter.() -> InboxFilter) {
        filters.update(transform)
        mutableState.update { it.copy(filter = filters.value) }
    }

    private fun launchMutation(
        successRes: StringResource? = null,
        successArgs: List<Any> = emptyList(),
        block: suspend () -> Unit,
    ) {
        screenModelScope.launch {
            runCatching { block() }.fold(
                onSuccess = {
                    if (successRes != null) {
                        val message = if (successArgs.isEmpty()) {
                            getString(successRes)
                        } else {
                            getString(successRes, *successArgs.toTypedArray())
                        }
                        mutableState.update { it.copy(message = message) }
                    }
                },
                onFailure = { error ->
                    mutableState.update {
                        it.copy(message = error.message ?: getString(Res.string.error_action_failed))
                    }
                },
            )
        }
    }
}
