package ai.saniou.nmb.workflow.forum

import ai.saniou.nmb.workflow.forum.ForumContract.Effect
import ai.saniou.nmb.workflow.forum.ForumContract.Event
import ai.saniou.nmb.workflow.forum.ForumContract.State
import ai.saniou.thread.data.source.nmb.DataPolicy
import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.usecase.forum.GetForumDetailUseCase
import ai.saniou.thread.domain.usecase.forum.GetForumNameUseCase
import ai.saniou.thread.domain.usecase.forum.GetForumThreadsPagingUseCase
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ForumViewModel(
    getForumThreadsPagingUseCase: GetForumThreadsPagingUseCase,
    getForumDetailUseCase: GetForumDetailUseCase,
    getForumNameUseCase: GetForumNameUseCase,
    private val forumId: Long,
    private val fgroupId: Long,
) : ScreenModel {

    private data class LoadRequest(
        val fid: Long,
        val fgroup: Long,
        val policy: DataPolicy = DataPolicy.NETWORK_ELSE_CACHE,
        val page: Int = 1,
    )

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadParams = MutableStateFlow(LoadRequest(fid = forumId, fgroup = fgroupId))
    private val showInfoDialog = MutableStateFlow(false)

    val threads: Flow<PagingData<Post>> =
        loadParams.flatMapLatest { request ->
            getForumThreadsPagingUseCase(
                fid = request.fid,
                isTimeline = request.fgroup == -1L,
                initialPage = request.page
            )
        }.cachedIn(screenModelScope)

    val state: StateFlow<State> = combine(
        getForumNameUseCase(forumId),
        getForumDetailUseCase(forumId),
        showInfoDialog
    ) { forumName, forumDetail, showDialog ->
        State(
            forumName = forumName ?: "",
            forumDetail = forumDetail,
            threads = threads,
            showInfoDialog = showDialog
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), State(threads = threads))


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
                showInfoDialog.value = event.show
            }
        }
    }
}
