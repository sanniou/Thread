package ai.saniou.forum.workflow.topic

import ai.saniou.forum.workflow.topic.TopicContract.Effect
import ai.saniou.forum.workflow.topic.TopicContract.Event
import ai.saniou.forum.workflow.topic.TopicContract.State
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.domain.model.forum.Topic as Post
import ai.saniou.thread.domain.usecase.channel.GetChannelDetailUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelTopicsPagingUseCase
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
class TopicViewModel(
    getChannelTopicsPagingUseCase: GetChannelTopicsPagingUseCase,
    getChannelDetailUseCase: GetChannelDetailUseCase,
    getChannelNameUseCase: GetChannelNameUseCase,
    private val sourceId: String,
    private val forumId: String,
    private val fgroupId: String,
) : ScreenModel {

    private data class LoadRequest(
        val fid: String,
        val fgroup: String,
        val policy: DataPolicy = DataPolicy.NETWORK_ELSE_CACHE,
        val page: Int = 1,
    )

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadParams = MutableStateFlow(LoadRequest(fid = forumId, fgroup = fgroupId))
    private val showInfoDialog = MutableStateFlow(false)

    val threads: Flow<PagingData<Post>> =
        loadParams.flatMapLatest { request ->
            getChannelTopicsPagingUseCase(
                sourceId = sourceId,
                fid = request.fid,
                isTimeline = request.fgroup == "-1",
                initialPage = request.page
            )
        }.cachedIn(screenModelScope)

    val state: StateFlow<State> = combine(
        getChannelNameUseCase(sourceId, forumId),
        getChannelDetailUseCase(sourceId, forumId),
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
