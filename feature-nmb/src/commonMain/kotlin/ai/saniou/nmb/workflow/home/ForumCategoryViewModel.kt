package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.domain.ForumCategoryUserCase
import ai.saniou.nmb.initializer.AppInitializer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumCategoryViewModel(
    private val forumCategoryUserCase: ForumCategoryUserCase,
    private val appInitializer: AppInitializer,
    private val categoryStorage: CategoryStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ForumCategoryUiState(
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
                        categoryStorage.saveLastOpenedCategoryId(newExpandId)
                    }
                    state.copy(
                        expandCategory = if (state.expandCategory == id) null else id
                    )
                }
            },
            onForumClick = { id ->
                // 保存选中的论坛ID
                viewModelScope.launch {
                    categoryStorage.saveLastOpenedForumId(id)
                }
                updateUiState { state ->
                    state.copy(
                        currentForum = id
                    )
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
            try {
                // 尝试从缓存加载
                val cachedCategories = categoryStorage.getCachedCategories()
                val lastOpenedCategoryId = categoryStorage.getLastOpenedCategoryId()
                val lastOpenedForumId = categoryStorage.getLastOpenedForumId()

                if (cachedCategories != null && !categoryStorage.isCategoryDataExpired()) {
                    // 使用缓存数据
                    updateUiState { state ->
                        state.copy(
                            forums = cachedCategories,
                            expandCategory = lastOpenedCategoryId,
                            currentForum = lastOpenedForumId
                        )
                    }
                } else {
                    // 从网络加载新数据
                    val forums = forumCategoryUserCase()

                    // 保存到缓存
                    categoryStorage.saveCategories(forums)

                    updateUiState { state ->
                        state.copy(
                            forums = forums,
                            expandCategory = lastOpenedCategoryId,
                            currentForum = lastOpenedForumId
                        )
                    }
                }
            } catch (e: Exception) {
                // 如果加载失败但有缓存，使用缓存
                val cachedCategories = categoryStorage.getCachedCategories()
                if (cachedCategories != null) {
                    val lastOpenedCategoryId = categoryStorage.getLastOpenedCategoryId()
                    val lastOpenedForumId = categoryStorage.getLastOpenedForumId()

                    updateUiState { state ->
                        state.copy(
                            forums = cachedCategories,
                            expandCategory = lastOpenedCategoryId,
                            currentForum = lastOpenedForumId
                        )
                    }
                }
                // 可以添加错误处理逻辑
            }
        }
    }

    private fun updateUiState(invoke: (ForumCategoryUiState) -> ForumCategoryUiState) {
        _uiState.update(invoke)
    }

    val uiState = _uiState.asStateFlow()
}

data class ForumCategoryUiState(
    var forums: List<ForumCategory>,
    var expandCategory: Long? = null,
    var currentForum: Long? = null,
    var checkState: Boolean,
    val onForwardChange: (Boolean) -> Unit,
    val onCategoryClick: (Long) -> Unit,
    val onForumClick: (Long) -> Unit,
)
