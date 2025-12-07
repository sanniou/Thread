package ai.saniou.nmb.workflow.forum

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import ai.saniou.nmb.data.repository.DataPolicy
import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.workflow.forum.ForumContract.Effect
import ai.saniou.nmb.workflow.forum.ForumContract.Event
import ai.saniou.nmb.workflow.forum.ForumContract.State
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ForumViewModel(
    private val forumUseCase: ForumUseCase,
    private val forumId: Long,
    private val fgroupId: Long
) : ScreenModel {

    private data class LoadRequest(
        val fid: Long,
        val fgroup: Long,
        val policy: DataPolicy = DataPolicy.API_FIRST,
        val page: Int = 1
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadParams = MutableStateFlow(LoadRequest(fid = forumId, fgroup = fgroupId))

    val threads: Flow<PagingData<ThreadWithInformation>> =
        loadParams.flatMapLatest { request ->
            forumUseCase(
                fid = request.fid,
                fgroup = request.fgroup,
                policy = request.policy,
                initialPage = request.page
            )
        }.cachedIn(screenModelScope)

    init {
        _state.update {
            it.copy(
                threads = threads,
                forumName = forumUseCase.getForumName(forumId),
                forumDetail = forumUseCase.getForumDetail(forumId)
            )
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.Refresh -> {
                // Refresh is handled by Paging library's refresh() method on the UI side
            }
            Event.ScrollToTop -> {
                screenModelScope.launch {
                    _effect.send(Effect.ScrollToTop)
                }
            }
            is Event.JumpToPage -> {
                loadParams.update { it.copy(page = event.page) }
            }
            is Event.ToggleInfoDialog -> {
                _state.update { it.copy(showInfoDialog = event.show) }
            }
        }
    }
}
