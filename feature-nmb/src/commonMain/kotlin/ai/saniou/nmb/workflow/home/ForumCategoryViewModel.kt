package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.domain.ForumCategoryUseCase
import ai.saniou.nmb.initializer.AppInitializer
import ai.saniou.nmb.workflow.home.ForumCategoryContract.Event
import ai.saniou.nmb.workflow.home.ForumCategoryContract.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumCategoryViewModel(
    private val forumCategoryUseCase: ForumCategoryUseCase,
    private val appInitializer: AppInitializer,
    private val categoryStorage: CategoryStorage
) : ViewModel() {

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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 初始化应用
                appInitializer.initialize()

                // 尝试从缓存加载
                val lastOpenedForum = categoryStorage.getLastOpenedForum()
                // 从网络加载新数据
                combine(
                    forumCategoryUseCase.getFavoriteForums(),
                    forumCategoryUseCase()
                ) { favorites, categories ->
                    val favoriteCategory = ForumCategory(
                        id = -2L, // 使用一个固定的特殊ID
                        sort = -2L,
                        name = "收藏",
                        status = "n",
                        forums = favorites
                    )
                    val combined = listOf(favoriteCategory) + categories
                    val favoriteIds = favorites.map { it.id }.toSet()
                    Pair(combined, favoriteIds)
                }.catch { e ->
                    _state.update { it.copy(error = "加载板块列表失败: ${e.message}") }
                }.collect { (combinedCategories, favoriteIds) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            categories = combinedCategories,
                            expandedCategoryId = lastOpenedForum?.fGroup,
                            currentForum = lastOpenedForum,
                            favoriteForumIds = favoriteIds
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "加载板块列表失败: ${e.message}") }
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

    private fun selectForum(forum: ai.saniou.nmb.data.entity.ForumDetail) {
        viewModelScope.launch {
            categoryStorage.saveLastOpenedForum(forum)
        }
        _state.update { it.copy(currentForum = forum) }
    }

    private fun toggleFavorite(forum: ai.saniou.nmb.data.entity.ForumDetail) {
        viewModelScope.launch {
            val isCurrentlyFavorite = state.value.favoriteForumIds.contains(forum.id)
            forumCategoryUseCase.changeFavoriteForum(forum)
            val message = if (isCurrentlyFavorite) "已取消收藏 ${forum.name}" else "已收藏 ${forum.name}"
            _state.update { it.copy(toastMessage = message) }
        }
    }
}
