package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.initializer.AppInitializer
import ai.saniou.forum.workflow.home.ChannelContract.Event
import ai.saniou.forum.workflow.home.ChannelContract.ChannelUiState
import ai.saniou.forum.workflow.home.ChannelContract.ChannelCategoryUiState
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.ChannelRepository
import ai.saniou.thread.domain.usecase.channel.GetFavoriteChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelsUseCase
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.domain.usecase.notice.GetNoticeUseCase
import ai.saniou.thread.domain.usecase.notice.MarkNoticeAsReadUseCase
import ai.saniou.thread.domain.usecase.post.ToggleFavoriteUseCase
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChannelViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val appInitializer: AppInitializer,
    private val channelRepository: ChannelRepository,
    private val getNoticeUseCase: GetNoticeUseCase,
    private val markNoticeAsReadUseCase: MarkNoticeAsReadUseCase,
    private val settingsRepository: SettingsRepository,
    private val getAvailableSourcesUseCase: GetAvailableSourcesUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(ChannelUiState())
    val state = _state.asStateFlow()

    private var initCheckJob: Job? = null

    init {
        loadSources()
        fetchNotice()
    }

    private fun loadSources() {
        screenModelScope.launch {
            val sources = getAvailableSourcesUseCase()
            val lastSourceId =
                settingsRepository.getValue("current_source_id") ?: sources.first().id
            val initialSourceId =
                if (sources.any { it.id == lastSourceId }) lastSourceId else sources.firstOrNull()?.id
                    ?: "nmb"

            _state.update {
                it.copy(
                    availableSources = sources,
                    currentSourceId = initialSourceId
                )
            }
            observeSourceInitialization(initialSourceId)
            onEvent(Event.LoadCategories)
        }
    }

    private fun observeSourceInitialization(sourceId: String) {
        initCheckJob?.cancel()
        initCheckJob = screenModelScope.launch {
            val source = state.value.availableSources.find { it.id == sourceId } ?: return@launch
            source.isInitialized.collect { isInitialized ->
                _state.update { it.copy(isCurrentSourceInitialized = isInitialized) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.LoadCategories -> loadCategories()
            is Event.SelectChannel -> selectForum(event.channel)
            Event.SelectHome -> selectHome()
            is Event.ToggleCategory -> toggleCategory(event.groupId)
            is Event.ToggleFavorite -> toggleFavorite(event.channel)
            Event.ToastShown -> onToastShown()
            Event.MarkNoticeRead -> markNoticeAsRead()
            is Event.SelectSource -> selectSource(event.sourceId)
        }
    }

    private fun selectSource(sourceId: String) {
        if (state.value.currentSourceId == sourceId) return
        screenModelScope.launch {
            settingsRepository.saveValue("current_source_id", sourceId)
        }
        _state.update { it.copy(currentSourceId = sourceId) }
        observeSourceInitialization(sourceId)
        loadCategories()
    }

    private fun fetchNotice() {
        screenModelScope.launch {
            getNoticeUseCase().collect { notice ->
                _state.update { it.copy(notice = notice) }
            }
        }
    }

    private fun markNoticeAsRead() {
        screenModelScope.launch {
            state.value.notice?.id?.let {
                markNoticeAsReadUseCase(it)
            }
        }
    }

    private fun loadCategories() {
        screenModelScope.launch {
            _state.update { it.copy(categoriesState = UiStateWrapper.Loading) }
            // App initialization should be handled globally, not here.
            // For now, we keep it to avoid breaking things.
            appInitializer.initialize()

            // TODO: Replace with a UseCase
            val lastOpenedForum = channelRepository.getLastOpenedChannel()
            // 如果最后打开的板块不是当前源的，则不选中
            val lastOpenedForumId =
                if (lastOpenedForum?.sourceName == state.value.currentSourceId) lastOpenedForum.id else null
            val currentSourceId = state.value.currentSourceId

            // Use Dispatchers.IO for database/network calls
            val forumsFlow = getChannelsUseCase(currentSourceId)
            val favoritesFlow = getFavoriteChannelsUseCase(currentSourceId)

            // Trigger fetch
            launch {
                channelRepository.fetchChannels(currentSourceId)
                    .onFailure { e ->
                        // Only show error if we don't have data yet? Or maybe show a toast?
                        // For now, we rely on the flow to show data if available.
                        // If flow is empty and fetch fails, we might want to show error state.
                        if (_state.value.categoriesState is UiStateWrapper.Loading) {
                            _state.update {
                                it.copy(
                                    categoriesState = UiStateWrapper.Error(e.toAppError { loadCategories() })
                                )
                            }
                        }
                    }
            }

            forumsFlow.combine(favoritesFlow) { forums, favorites ->
                Pair(forums, favorites)
            }.catch { e ->
                _state.update {
                    it.copy(
                        categoriesState = UiStateWrapper.Error(e.toAppError { loadCategories() })
                    )
                }
            }.collect { (forums, favorites) ->
                val favoriteGroup = ChannelCategoryUiState(
                    id = "-2", // Special ID for favorites
                    name = "收藏",
                    channels = favorites
                )

                val forumGroups = forums
                    .groupBy { it.groupName }
                    .mapNotNull { (groupName, forumList) ->
                        if (groupName.isBlank()) return@mapNotNull null
                        val firstForum = forumList.first()
                        ChannelCategoryUiState(
                            id = firstForum.groupId,
                            name = groupName,
                            channels = forumList
                        )
                    }

                val combined = listOf(favoriteGroup) + forumGroups
                val favoriteIds = favorites.map { it.id }.toSet()

                _state.update {
                    val isInitialLoad =
                        it.categoriesState is UiStateWrapper.Loading || it.categoriesState is UiStateWrapper.Error
                    val allForums = forums + favorites
                    // 找到最后打开的板块对象，确保它在当前列表中
                    val lastOpenedForumObj =
                        if (isInitialLoad) allForums.find { f -> f.id == lastOpenedForumId } else null

                    it.copy(
                        categoriesState = UiStateWrapper.Success(combined),
                        expandedGroupId = if (isInitialLoad) lastOpenedForumObj?.groupId else it.expandedGroupId,
                        currentChannel = if (isInitialLoad) lastOpenedForumObj else it.currentChannel,
                        favoriteChannelIds = favoriteIds
                    )
                }
            }
        }
    }

    private fun onToastShown() {
        _state.update { it.copy(toastMessage = null) }
    }

    private fun toggleCategory(groupId: String) {
        _state.update {
            it.copy(
                expandedGroupId = if (it.expandedGroupId == groupId) null else groupId
            )
        }
    }

    private fun selectForum(forum: Channel) {
        screenModelScope.launch {
            channelRepository.saveLastOpenedChannel(forum)
        }
        _state.update { it.copy(currentChannel = forum) }
    }

    private fun selectHome() {
        _state.update { it.copy(currentChannel = null) }
    }

    private fun toggleFavorite(forum: Channel) {
        screenModelScope.launch {
            val isCurrentlyFavorite = state.value.favoriteChannelIds.contains(forum.id)
            toggleFavoriteUseCase(state.value.currentSourceId, forum)
            val message =
                if (isCurrentlyFavorite) "已取消收藏 ${forum.name}" else "已收藏 ${forum.name}"
            _state.update { it.copy(toastMessage = message) }
        }
    }
}
