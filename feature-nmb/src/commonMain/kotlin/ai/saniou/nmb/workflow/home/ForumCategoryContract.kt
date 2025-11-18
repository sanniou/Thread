package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.ForumDetail

/**
 * 定义 ForumCategory 功能模块的 UI 状态和事件
 */
interface ForumCategoryContract {
    /**
     * UI 状态
     *
     * @property isLoading 是否正在加载板块列表
     * @property error 错误信息
     * @property categories 板块分类列表
     * @property expandedCategoryId 当前展开的分类 ID
     * @property currentForum 当前选中的板块
     * @property favoriteForumIds 收藏的板块ID列表
     * @property toastMessage 提示信息
     */
    data class State(
        val isLoading: Boolean = true,
        val error: String? = null,
        val categories: List<ForumCategory> = emptyList(),
        val expandedCategoryId: Long? = null,
        val currentForum: ForumDetail? = null,
        val favoriteForumIds: Set<Long> = emptySet(),
        val toastMessage: String? = null
    )

    /**
     * UI 事件
     */
    sealed interface Event {
        /**
         * 加载所有板块分类
         */
        object LoadCategories : Event

        /**
         * 切换分类的展开/折叠状态
         * @param categoryId 分类 ID
         */
        data class ToggleCategory(val categoryId: Long) : Event

        /**
         * 选中一个板块
         * @param forum 板块详情
         */
        data class SelectForum(val forum: ForumDetail) : Event

        /**
         * 切换一个板块的收藏状态
         * @param forum 板块详情
         */
        data class ToggleFavorite(val forum: ForumDetail) : Event

        /**
         * 提示信息已显示
         */
        object ToastShown : Event
    }
}
