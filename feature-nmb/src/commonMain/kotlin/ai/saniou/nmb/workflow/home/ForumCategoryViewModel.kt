package ai.saniou.nmb.workflow.home

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
import kotlinx.coroutines.flow.collect
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
                val categories = forumCategoryUseCase()

                _state.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        expandedCategoryId = lastOpenedForum?.fGroup,
                        currentForum = lastOpenedForum
                    )
                }

                // 监听收藏夹变化
                forumCategoryUseCase.getFavoriteForums()
                    .catch { e -> _state.update { it.copy(error = "加载收藏夹失败: ${e.message}") } }
                    .collect { favorites ->
                        _state.update { it.copy(favoriteForums = favorites) }
                    }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "加载板块列表失败: ${e.message}") }
            }
        }
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
            forumCategoryUseCase.changeFavoriteForum(forum)
        }
    }
}
