package ai.saniou.nmb.workflow.forum

import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.workflow.forum.ForumContract.Event
import ai.saniou.nmb.workflow.forum.ForumContract.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class ForumViewModel(private val forumUseCase: ForumUseCase) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val loadParams = MutableSharedFlow<Pair<Long, Long>>(replay = 1)

    val threads: Flow<PagingData<ai.saniou.nmb.data.entity.Forum>> =
        loadParams.flatMapLatest { (fid, fgroup) ->
            forumUseCase(fid, fgroup)
        }.cachedIn(viewModelScope)

    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadForum -> {
                loadParams.tryEmit(event.fid to event.fgroup)
                _state.update {
                    it.copy(
                        threads = threads,
                        forumName = forumUseCase.getForumName(event.fid)
                    )
                }
            }

            Event.Refresh -> {
                // Refresh is handled by Paging library's refresh() method on the UI side
            }
        }
    }
}
