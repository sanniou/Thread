package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.coreui.state.toAppError
import ai.saniou.forum.initializer.AppInitializer
import ai.saniou.forum.workflow.home.ChannelContract.ChannelCategoryUiState
import ai.saniou.forum.workflow.home.ChannelContract.ChannelUiState
import ai.saniou.forum.workflow.home.ChannelContract.Event
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.domain.usecase.channel.FetchChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetFavoriteChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetLastOpenedChannelUseCase
import ai.saniou.thread.domain.usecase.channel.GetRecentChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.SaveLastOpenedChannelUseCase
import ai.saniou.thread.domain.usecase.channel.SignFavoriteChannelsUseCase
import ai.saniou.thread.domain.usecase.notice.GetNoticeUseCase
import ai.saniou.thread.domain.usecase.notice.MarkNoticeAsReadUseCase
import ai.saniou.thread.domain.usecase.post.ToggleFavoriteUseCase
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.usecase.refresh.ObserveRefreshDiagnosticsUseCase
import ai.saniou.thread.domain.model.workspace.ForumWorkspaceState
import ai.saniou.thread.domain.model.workspace.ListAnchor
import ai.saniou.thread.domain.usecase.workspace.ObserveWorkspaceSessionUseCase
import ai.saniou.thread.domain.usecase.workspace.UpdateWorkspaceSessionUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_249b0d1cc6
import thread.feature_forum.generated.resources.s_5fafcc2093
import thread.feature_forum.generated.resources.s_980abb36c0
import thread.feature_forum.generated.resources.action_bookmark
import thread.feature_forum.generated.resources.s_0e055beabc

class ChannelViewModel(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val getFavoriteChannelsUseCase: GetFavoriteChannelsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val appInitializer: AppInitializer,
    private val getNoticeUseCase: GetNoticeUseCase,
    private val markNoticeAsReadUseCase: MarkNoticeAsReadUseCase,
    private val settingsRepository: SettingsRepository,
    private val getAvailableSourcesUseCase: GetAvailableSourcesUseCase,
    private val fetchChannelsUseCase: FetchChannelsUseCase,
    private val getLastOpenedChannelUseCase: GetLastOpenedChannelUseCase,
    private val getRecentChannelsUseCase: GetRecentChannelsUseCase,
    private val saveLastOpenedChannelUseCase: SaveLastOpenedChannelUseCase,
    private val observeRefreshDiagnosticsUseCase: ObserveRefreshDiagnosticsUseCase,
    private val observeWorkspaceSessionUseCase: ObserveWorkspaceSessionUseCase,
    private val updateWorkspaceSessionUseCase: UpdateWorkspaceSessionUseCase,
    private val signFavoriteChannelsUseCase: SignFavoriteChannelsUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(ChannelUiState())
    val state = _state.asStateFlow()

    private var initCheckJob: Job? = null
    private var categoriesJob: Job? = null
    private var restoredForum = ForumWorkspaceState()

    init {
        screenModelScope.launch {
            observeRefreshDiagnosticsUseCase().collect { tasks ->
                _state.update { current ->
                    current.copy(
                        refreshFailures = tasks.values.filter {
                            it.status == RefreshStatus.FAILED &&
                                it.key == "forum:${current.currentSourceId}:catalog"
                        },
                    )
                }
            }
        }
        screenModelScope.launch {
            restoredForum = observeWorkspaceSessionUseCase().first().forum
            _state.update {
                it.copy(
                    expandedGroupId = restoredForum.expandedGroupId,
                    listAnchor = restoredForum.listAnchor,
                )
            }
            loadSources()
        }
        fetchNotice()
    }

    private fun loadSources() {
        screenModelScope.launch {
            getAvailableSourcesUseCase().collect { sources ->
                if (sources.isEmpty()) {
                    _state.update {
                        it.copy(
                            availableSources = emptyList(),
                            currentSourceId = "",
                            isCurrentSourceInitialized = true,
                            categoriesState = UiStateWrapper.Success(emptyList()),
                            currentChannel = null,
                        )
                    }
                    return@collect
                }
                val savedSourceId = settingsRepository.getValue<String>("current_source_id")
                val currentId = state.value.currentSourceId
                val targetId = when {
                    sources.any { it.id == currentId } -> currentId
                    sources.any { it.id == restoredForum.sourceId } -> restoredForum.sourceId.orEmpty()
                    sources.any { it.id == savedSourceId } -> savedSourceId.orEmpty()
                    else -> sources.first().id
                }
                val sourceChanged = currentId != targetId
                _state.update {
                    it.copy(availableSources = sources, currentSourceId = targetId)
                }
                if (sourceChanged || state.value.categoriesState is UiStateWrapper.Loading) {
                    observeSourceInitialization(targetId)
                    loadCategories()
                }
            }
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
            is Event.ListPositionChanged -> persistListPosition(event)
            Event.SignFavorites -> signFavorites()
        }
    }

    private fun signFavorites() {
        val sourceId = state.value.currentSourceId
        if (sourceId.isBlank()) return
        screenModelScope.launch {
            val message = runCatching {
                signFavoriteChannelsUseCase(sourceId)
            }.getOrElse { error ->
                getString(Res.string.s_0e055beabc, error.message ?: error.toString())
            }
            _state.update { it.copy(toastMessage = message) }
        }
    }

    private fun selectSource(sourceId: String) {
        if (state.value.currentSourceId == sourceId) return
        screenModelScope.launch {
            settingsRepository.saveValue("current_source_id", sourceId)
        }
        _state.update { it.copy(currentSourceId = sourceId) }
        persistForumState(sourceId = sourceId, channelId = null, listAnchor = null)
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
        categoriesJob?.cancel()
        categoriesJob = screenModelScope.launch {
            _state.update { it.copy(categoriesState = UiStateWrapper.Loading) }
            appInitializer.initialize()

            val lastOpenedForum = getLastOpenedChannelUseCase()
            val lastOpenedForumId = when {
                restoredForum.sourceId == state.value.currentSourceId -> restoredForum.channelId
                lastOpenedForum?.sourceId == state.value.currentSourceId -> lastOpenedForum.id
                else -> null
            }
            val currentSourceId = state.value.currentSourceId

            val forumsFlow = getChannelsUseCase(currentSourceId)
            val favoritesFlow = getFavoriteChannelsUseCase(currentSourceId)
            val recentFlow = getRecentChannelsUseCase(currentSourceId)

            launch {
                fetchChannelsUseCase(currentSourceId)
                    .onFailure { e ->
                        if (_state.value.categoriesState is UiStateWrapper.Loading) {
                            _state.update {
                                it.copy(
                                    categoriesState = UiStateWrapper.Error(e.toAppError { loadCategories() })
                                )
                            }
                        }
                    }
            }

            combine(forumsFlow, favoritesFlow, recentFlow) { forums, favorites, recent ->
                Triple(forums, favorites, recent)
            }.catch { e ->
                _state.update {
                    it.copy(
                        categoriesState = UiStateWrapper.Error(e.toAppError { loadCategories() })
                    )
                }
            }.collect { (forums, favorites, recent) ->
                val favoriteGroup = ChannelCategoryUiState(
                    id = "-2", // Special ID for favorites
                    name = getString(Res.string.action_bookmark),
                    channels = favorites
                )
                val recentGroup = ChannelCategoryUiState(
                    id = "-3",
                    name = getString(Res.string.s_249b0d1cc6),
                    channels = recent,
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

                val combined = buildList {
                    if (recent.isNotEmpty()) add(recentGroup)
                    add(favoriteGroup)
                    addAll(forumGroups)
                }
                val favoriteIds = favorites.map { it.id }.toSet()

                _state.update {
                    val isInitialLoad =
                        it.categoriesState is UiStateWrapper.Loading || it.categoriesState is UiStateWrapper.Error
                    val allForums = forums + favorites
                    val lastOpenedForumObj =
                        if (isInitialLoad) allForums.find { f -> f.id == lastOpenedForumId } else null

                    it.copy(
                        categoriesState = UiStateWrapper.Success(combined),
                        expandedGroupId = if (isInitialLoad) {
                            lastOpenedForumObj?.groupId ?: it.expandedGroupId
                        } else {
                            it.expandedGroupId
                        },
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
        persistForumState(expandedGroupId = state.value.expandedGroupId)
    }

    private fun selectForum(forum: Channel) {
        screenModelScope.launch {
            saveLastOpenedChannelUseCase(forum)
        }
        _state.update { it.copy(currentChannel = forum) }
        persistForumState(
            sourceId = forum.sourceId,
            channelId = forum.id,
            expandedGroupId = forum.groupId,
            listAnchor = state.value.listAnchor?.takeIf { it.contextKey == "${forum.sourceId}:${forum.id}" },
        )
    }

    private fun selectHome() {
        _state.update { it.copy(currentChannel = null) }
        persistForumState(channelId = null, listAnchor = null)
    }

    private fun persistListPosition(event: Event.ListPositionChanged) {
        val anchor = ListAnchor(event.contextKey, event.index, event.offset)
        _state.update { it.copy(listAnchor = anchor) }
        persistForumState(listAnchor = anchor)
    }

    private fun persistForumState(
        sourceId: String? = state.value.currentSourceId.takeIf(String::isNotBlank),
        channelId: String? = state.value.currentChannel?.id,
        expandedGroupId: String? = state.value.expandedGroupId,
        listAnchor: ListAnchor? = state.value.listAnchor,
    ) {
        screenModelScope.launch {
            updateWorkspaceSessionUseCase { session ->
                session.copy(
                    forumSourceId = sourceId,
                    forum = session.forum.copy(
                        sourceId = sourceId,
                        channelId = channelId,
                        expandedGroupId = expandedGroupId,
                        listAnchor = listAnchor,
                    ),
                    updatedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    private fun toggleFavorite(forum: Channel) {
        screenModelScope.launch {
            val isCurrentlyFavorite = state.value.favoriteChannelIds.contains(forum.id)
            toggleFavoriteUseCase(state.value.currentSourceId, forum)
            val message =
                if (isCurrentlyFavorite) getString(Res.string.s_5fafcc2093, forum.name) else getString(Res.string.s_980abb36c0, forum.name)
            _state.update { it.copy(toastMessage = message) }
        }
    }
}
