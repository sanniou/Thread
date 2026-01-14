package ai.saniou.forum.workflow.topic

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.topic.TopicContract.Effect
import ai.saniou.forum.workflow.topic.TopicContract.Event
import ai.saniou.forum.workflow.topic.TopicContract.State
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.usecase.channel.GetChannelDetailUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelTopicsPagingUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val channelId: String,
    private val channelCategoryId: String,
) : ScreenModel {

    private data class LoadRequest(
        val channelId: String,
        val channelCategory: String,
        val policy: DataPolicy = DataPolicy.NETWORK_ELSE_CACHE,
        val page: Int = 1,
    )

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadParams = MutableStateFlow(LoadRequest(channelId = channelId, channelCategory = channelCategoryId))
    private val showInfoDialog = MutableStateFlow(false)

    val topics: Flow<PagingData<Topic>> =
        loadParams.flatMapLatest { request ->
            getChannelTopicsPagingUseCase(
                sourceId = sourceId,
                channelId = request.channelId,
                isTimeline = request.channelCategory == "-1",
                initialPage = request.page
            )
        }.cachedIn(screenModelScope)

    private val forumDetailFlow = getChannelDetailUseCase(sourceId, channelId)
        .map { UiStateWrapper.Success(it) as UiStateWrapper<ai.saniou.thread.domain.model.forum.Channel> }
        .onStart { emit(UiStateWrapper.Loading) }
        .catch { emit(UiStateWrapper.Error(it.toAppError())) }

    val state: StateFlow<State> = combine(
        getChannelNameUseCase(sourceId, channelId),
        forumDetailFlow,
        showInfoDialog
    ) { forumName, forumDetail, showDialog ->
        State(
            channelName = forumName ?: "",
            channelDetail = forumDetail,
            topics = topics,
            showInfoDialog = showDialog
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), State(topics = topics))


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
