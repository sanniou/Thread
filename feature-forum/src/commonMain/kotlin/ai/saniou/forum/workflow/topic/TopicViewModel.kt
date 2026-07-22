package ai.saniou.forum.workflow.topic

import ai.saniou.coreui.state.AppError
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.workflow.topic.TopicContract.Effect
import ai.saniou.forum.workflow.topic.TopicContract.Event
import ai.saniou.forum.workflow.topic.TopicContract.State
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.ForumRuleDetail
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.usecase.channel.GetChannelDetailUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelTopicsPagingUseCase
import ai.saniou.thread.domain.usecase.channel.GetForumRulesUseCase
import ai.saniou.thread.domain.usecase.channel.SetChannelFallbackModeUseCase
import ai.saniou.thread.domain.usecase.channel.SignChannelUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel as EventChannel
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
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_0e055beabc
import thread.feature_forum.generated.resources.s_43b4a10a57

@OptIn(ExperimentalCoroutinesApi::class)
class TopicViewModel(
    getChannelTopicsPagingUseCase: GetChannelTopicsPagingUseCase,
    private val setChannelFallbackModeUseCase: SetChannelFallbackModeUseCase,
    getChannelDetailUseCase: GetChannelDetailUseCase,
    getChannelNameUseCase: GetChannelNameUseCase,
    sourceRepository: SourceRepository,
    private val signChannelUseCase: SignChannelUseCase,
    private val getForumRulesUseCase: GetForumRulesUseCase,
    private val sourceId: String,
    private val channelId: String,
    private val channelCategoryId: String,
) : ScreenModel {

    private data class LoadRequest(
        val channelId: String,
        val channelCategory: String,
        val page: Int = 1,
    )

    private data class ActionUi(
        val actionMessage: String? = null,
        val isSigning: Boolean = false,
        val forumRules: ForumRuleDetail? = null,
        val isLoadingRules: Boolean = false,
        val showRulesDialog: Boolean = false,
    )

    private val _effect = EventChannel<Effect>()
    val effect = _effect.receiveAsFlow()

    private val loadParams = MutableStateFlow(LoadRequest(channelId = channelId, channelCategory = channelCategoryId))
    private val showInfoDialog = MutableStateFlow(false)
    private val actionUi = MutableStateFlow(ActionUi())
    private val capabilities = sourceRepository.getSource(sourceId)?.capabilities
        ?: ai.saniou.thread.domain.model.SourceCapabilities.Default

    val topics: Flow<PagingData<Topic>> =
        loadParams.flatMapLatest { request ->
            getChannelTopicsPagingUseCase(
                sourceId = sourceId,
                channelId = request.channelId,
                isTimeline = request.channelCategory == "-1",
                initialPage = request.page,
            )
        }.cachedIn(screenModelScope)

    private val forumDetailFlow: Flow<UiStateWrapper<Channel>> =
        getChannelDetailUseCase(sourceId, channelId)
            .map { channel ->
                channel?.let { UiStateWrapper.Success(it) }
                    ?: UiStateWrapper.Error(AppError(message = getString(Res.string.s_43b4a10a57)))
            }
            .onStart { emit(UiStateWrapper.Loading) }
            .catch { emit(UiStateWrapper.Error(it.toAppError())) }

    val state: StateFlow<State> = combine(
        getChannelNameUseCase(sourceId, channelId),
        forumDetailFlow,
        showInfoDialog,
        actionUi,
    ) { forumName, forumDetail, showDialog, actions ->
        State(
            channelName = forumName ?: "",
            channelDetail = forumDetail,
            topics = topics,
            capabilities = capabilities,
            showInfoDialog = showDialog,
            actionMessage = actions.actionMessage,
            isSigning = actions.isSigning,
            forumRules = actions.forumRules,
            isLoadingRules = actions.isLoadingRules,
            showRulesDialog = actions.showRulesDialog,
        )
    }.stateIn(screenModelScope, SharingStarted.WhileSubscribed(5000), State(topics = topics))

    fun onEvent(event: Event) {
        when (event) {
            Event.Refresh -> Unit

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

            Event.ShowCache -> {
                screenModelScope.launch {
                    setChannelFallbackModeUseCase(sourceId, channelId, true)
                }
            }

            Event.SignChannel -> signChannel()

            Event.LoadForumRules -> loadForumRules()

            is Event.ToggleRulesDialog -> {
                actionUi.update { it.copy(showRulesDialog = event.show) }
            }

            Event.ActionMessageShown -> {
                actionUi.update { it.copy(actionMessage = null) }
            }
        }
    }

    private fun signChannel() {
        if (!capabilities.supportsChannelSign || actionUi.value.isSigning) return
        screenModelScope.launch {
            actionUi.update { it.copy(isSigning = true) }
            val channel = (state.value.channelDetail as? UiStateWrapper.Success)?.value
                ?: Channel(
                    id = channelId,
                    name = state.value.channelName.ifBlank { channelId },
                    displayName = state.value.channelName.ifBlank { channelId },
                    description = "",
                    descriptionText = null,
                    groupId = channelCategoryId,
                    groupName = "",
                    sourceName = sourceId,
                    sort = null,
                    topicCount = null,
                    postCount = null,
                    autoDelete = null,
                    sourceId = sourceId,
                )
            val message = runCatching {
                signChannelUseCase(sourceId, channel)
            }.getOrElse { error ->
                getString(Res.string.s_0e055beabc, error.message ?: error.toString())
            }
            actionUi.update { it.copy(actionMessage = message, isSigning = false) }
        }
    }

    private fun loadForumRules() {
        if (!capabilities.supportsForumRules || actionUi.value.isLoadingRules) return
        screenModelScope.launch {
            actionUi.update { it.copy(isLoadingRules = true, showRulesDialog = true) }
            val result = runCatching {
                getForumRulesUseCase(sourceId, channelId)
            }
            result.onSuccess { detail ->
                actionUi.update {
                    it.copy(
                        forumRules = detail,
                        isLoadingRules = false,
                        showRulesDialog = true,
                    )
                }
            }.onFailure { error ->
                actionUi.update {
                    it.copy(
                        actionMessage = getString(
                            Res.string.s_0e055beabc,
                            error.message ?: error.toString(),
                        ),
                        isLoadingRules = false,
                        showRulesDialog = false,
                    )
                }
            }
        }
    }
}
