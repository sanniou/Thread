package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.ForumDetail
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.domain.ForumCategoryUseCase
import ai.saniou.nmb.initializer.AppInitializer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumCategoryViewModel(
    private val forumCategoryUseCase: ForumCategoryUseCase,
    private val appInitializer: AppInitializer,
    private val categoryStorage: CategoryStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ForumCategoryUiState(
            favoriteForums = MutableStateFlow(emptyList()),
            forums = emptyList(),
            checkState = false,
            onForwardChange = { checked ->
                updateUiState {
                    it.copy(checkState = checked)
                }
            },
            onCategoryClick = { id ->
                updateUiState { state ->
                    // 保存展开的分类ID
                    viewModelScope.launch {
                        val newExpandId = if (state.expandCategory == id) null else id
                    }
                    state.copy(
                        expandCategory = if (state.expandCategory == id) null else id
                    )
                }
            },
            onForumClick = { forum ->
                // 保存选中的论坛ID
                viewModelScope.launch {
                    categoryStorage.saveLastOpenedForum(forum)
                }
                updateUiState { state ->
                    state.copy(
                        currentForum = forum
                    )
                }
            },
            onFavoriteChange = { forum ->
                viewModelScope.launch {
                    forumCategoryUseCase.changeFavoriteForum(forum)
                }
            }
        )
    )

    init {
        // 初始化应用
        appInitializer.initialize()

        // 加载论坛分类
        loadForumCategories()
    }

    private fun loadForumCategories() {
        viewModelScope.launch {
            // 尝试从缓存加载
            val lastOpenedForum = categoryStorage.getLastOpenedForum()
            val favoriteForums = forumCategoryUseCase.getFavoriteForums()
            // 从网络加载新数据
            val forums = forumCategoryUseCase()

            updateUiState { state ->
                state.copy(
                    favoriteForums = favoriteForums,
                    forums = forums,
                    expandCategory = lastOpenedForum?.fGroup,
                    currentForum = lastOpenedForum
                )
            }
        }
    }

    private fun updateUiState(invoke: (ForumCategoryUiState) -> ForumCategoryUiState) {
        _uiState.update(invoke)
    }

    val uiState = _uiState.asStateFlow()
}

data class ForumCategoryUiState(
    var favoriteForums: Flow<List<ForumDetail>>,
    var forums: List<ForumCategory>,
    var expandCategory: Long? = null,
    var currentForum: ForumDetail? = null,
    var checkState: Boolean,
    val onForwardChange: (Boolean) -> Unit,
    val onCategoryClick: (Long) -> Unit,
    val onForumClick: (ForumDetail) -> Unit,
    val onFavoriteChange: (ForumDetail) -> Unit,
)
