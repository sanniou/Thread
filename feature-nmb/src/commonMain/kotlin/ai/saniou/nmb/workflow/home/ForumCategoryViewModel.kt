package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.initializer.AppInitializer
import ai.saniou.nmb.workflow.home.ForumCategoryContract.Event
import ai.saniou.nmb.workflow.home.ForumCategoryContract.ForumGroupUiState
import ai.saniou.nmb.workflow.home.ForumCategoryContract.State
import ai.saniou.thread.data.source.nmb.remote.dto.ForumDetail
import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.usecase.GetNmbForumPageUseCase
import ai.saniou.thread.domain.usecase.ToggleFavoriteUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumCategoryViewModel(
    private val getNmbForumPageUseCase: GetNmbForumPageUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val appInitializer: AppInitializer,
    private val categoryStorage: CategoryStorage, // TODO: This dependency should be removed later
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        onEvent(Event.LoadCategories)
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.LoadCategories -> loadCategories()
            is Event.SelectForum -> selectForum(event.forum)
            is Event.ToggleCategory -> toggleCategory(event.groupId)
            is Event.ToggleFavorite -> toggleFavorite(event.forum)
            Event.ToastShown -> onToastShown()
        }
    }

    private fun loadCategories() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            // App initialization should be handled globally, not here.
            // For now, we keep it to avoid breaking things.
            appInitializer.initialize()

            // TODO: Replace with a UseCase
            val lastOpenedForumId = categoryStorage.getLastOpenedForum()?.id?.toString()

            getNmbForumPageUseCase()
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "加载板块列表失败: ${e.message}"
                        )
                    }
                }
                .collect { result ->
                    result.onSuccess { data ->
                        val favoriteGroup = ForumGroupUiState(
                            id = "-2", // Special ID for favorites
                            name = "收藏",
                            forums = data.favorites
                        )

                        val forumGroups = data.forums
                            .groupBy { it.groupName }
                            .mapNotNull { (groupName, forums) ->
                                if (groupName.isBlank()) return@mapNotNull null
                                val firstForum = forums.first()
                                ForumGroupUiState(
                                    id = firstForum.groupId,
                                    name = groupName,
                                    forums = forums
                                )
                            }

                        val combined = listOf(favoriteGroup) + forumGroups
                        val favoriteIds = data.favorites.map { it.id }.toSet()

                        _state.update {
                            val isInitialLoad = it.forumGroups.isEmpty()
                            val allForums = data.forums + data.favorites
                            val lastOpenedForum = if (isInitialLoad) allForums.find { f -> f.id == lastOpenedForumId } else null

                            it.copy(
                                isLoading = false,
                                forumGroups = combined,
                                expandedGroupId = if (isInitialLoad) lastOpenedForum?.groupId else it.expandedGroupId,
                                currentForum = if (isInitialLoad) lastOpenedForum else it.currentForum,
                                favoriteForumIds = favoriteIds
                            )
                        }
                    }.onFailure { e ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "加载板块列表失败: ${e.message}"
                            )
                        }
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

    private fun selectForum(forum: Forum) {
        screenModelScope.launch {
            // This is not ideal, the storage logic should be in a UseCase.
            // A temporary mapper is needed until storage is also refactored.
            val legacyForum = ForumDetail(
                id = forum.id.toLong(),
                name = forum.name,
                showName = forum.showName,
                msg = forum.msg,
                fGroup = forum.groupId.toLong()
            )
            categoryStorage.saveLastOpenedForum(legacyForum)
        }
        _state.update { it.copy(currentForum = forum) }
    }

    private fun toggleFavorite(forum: Forum) {
        screenModelScope.launch {
            val isCurrentlyFavorite = state.value.favoriteForumIds.contains(forum.id)
            toggleFavoriteUseCase("nmb", forum)
            val message =
                if (isCurrentlyFavorite) "已取消收藏 ${forum.name}" else "已收藏 ${forum.name}"
            _state.update { it.copy(toastMessage = message) }
        }
    }
}
