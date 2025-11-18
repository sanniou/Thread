package ai.saniou.nmb.workflow.forum

import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.workflow.forum.ForumContract.Effect
import ai.saniou.nmb.workflow.forum.ForumContract.Event
import ai.saniou.nmb.workflow.forum.ForumContract.State
import androidx.paging.PagingData
import androidx.paging.cachedIn
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

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadParams = MutableStateFlow(forumId to fgroupId)

    val threads: Flow<PagingData<ai.saniou.nmb.data.entity.Forum>> =
        loadParams.flatMapLatest { (fid, fgroup) ->
            forumUseCase(fid, fgroup)
        }.cachedIn(screenModelScope)

    init {
        _state.update {
            it.copy(
                threads = threads,
                forumName = forumUseCase.getForumName(forumId)
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
        }
    }
}
