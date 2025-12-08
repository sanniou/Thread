package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.initializer.AppInitializer
import ai.saniou.nmb.workflow.home.ForumCategoryContract.Event
import ai.saniou.nmb.workflow.home.ForumCategoryContract.State
import ai.saniou.thread.data.source.nmb.remote.dto.ForumCategory
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
            is Event.ToggleCategory -> toggleCategory(event.categoryId)
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

            val lastOpenedForum = categoryStorage.getLastOpenedForum()

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
                        val favoriteCategory = ForumCategory(
                            id = -2L, // Special ID for favorites
                            sort = -2L,
                            name = "收藏",
                            status = "n",
                            forums = data.favorites.map { it.toForumDetail() }
                        )
                        val combined = listOf(favoriteCategory) + data.forums.toForumCategories()
                        val favoriteIds = data.favorites.map { it.id.toLong() }.toSet()

                        _state.update {
                            val isInitialLoad = it.categories.isEmpty()
                            it.copy(
                                isLoading = false,
                                categories = combined,
                                expandedCategoryId = if (isInitialLoad) lastOpenedForum?.fGroup else it.expandedCategoryId,
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

    private fun toggleCategory(categoryId: Long) {
        _state.update {
            it.copy(
                expandedCategoryId = if (it.expandedCategoryId == categoryId) null else categoryId
            )
        }
    }

    private fun selectForum(forum: ForumDetail) {
        screenModelScope.launch {
            // This is not ideal, the storage logic should be in a UseCase.
            categoryStorage.saveLastOpenedForum(forum.toLegacyForumDetail())
        }
        _state.update { it.copy(currentForum = forum) }
    }

    private fun toggleFavorite(forumDetail: ForumDetail) {
        screenModelScope.launch {
            val isCurrentlyFavorite = state.value.favoriteForumIds.contains(forumDetail.id)

            val groupName = state.value.categories
                .find { it.id == forumDetail.fGroup }?.name ?: ""

            toggleFavoriteUseCase("nmb", forumDetail.toDomainForum(groupName))
            val message =
                if (isCurrentlyFavorite) "已取消收藏 ${forumDetail.name}" else "已收藏 ${forumDetail.name}"
            _state.update { it.copy(toastMessage = message) }
        }
    }

    // Mapper functions to adapt new domain models to old UI models.
    // These should be removed once the UI is updated to use the new models directly.
    private fun Forum.toForumDetail(): ForumDetail {
        return ForumDetail(
            id = this.id.toLong(),
            name = this.name,
            showName = this.showName,
            msg = this.msg,
            fGroup = this.groupId.toLong(),
            sort = 0,
            threadCount = 0
        )
    }

    private fun List<Forum>.toForumCategories(): List<ForumCategory> {
        return this.groupBy { it.groupName }
            .mapNotNull { (groupName, forums) ->
                // groupName could be empty if a forum doesn't belong to any category, filter it out.
                if (groupName.isBlank()) return@mapNotNull null

                val firstForumInGroup = forums.first()
                ForumCategory(
                    id = firstForumInGroup.groupId.toLong(),
                    sort = 0L,
                    status = "n",
                    name = groupName,
                    forums = forums.map { it.toForumDetail() }
                )
            }
    }

    private fun ForumDetail.toDomainForum(groupName: String): Forum {
        return Forum(
            id = this.id.toString(),
            name = this.name,
            showName = this.showName,
            msg = this.msg,
            groupId = this.fGroup.toString(),
            groupName = groupName,
            sourceName = "nmb",
            tag = if (this.fGroup == -1L) "isTimeLine" else null,
        )
    }

    private fun ForumDetail.toLegacyForumDetail(): ForumDetail {
        return ForumDetail(
            id = this.id,
            name = this.name,
            showName = this.showName,
            msg = this.msg,
            fGroup = this.fGroup,
            sort = 0, // Sort info is lost
            threadCount = 0 // Count is lost
        )
    }
}
