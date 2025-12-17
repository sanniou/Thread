package ai.saniou.forum.workflow.home

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.thread.domain.model.forum.Forum
import ai.saniou.thread.domain.repository.Source

/**
 * 定义 ForumCategory 功能模块的 UI 状态和事件
 */
interface ForumCategoryContract {

    /**
     * 用于UI显示的板块分组
     *
     * @param id 分组的唯一ID (来自 Forum.groupId)
     * @param name 分组的名称 (来自 Forum.groupName)
     * @param forums 该分组下的板块列表
     */
    data class ForumGroupUiState(
        val id: String,
        val name: String,
        val forums: List<Forum>
    )

    /**
     * UI 状态
     *
     * @property categoriesState 板块列表状态 (Loading/Success/Error)
     * @property expandedGroupId 当前展开的分组 ID
     * @property currentForum 当前选中的板块
     * @property favoriteForumIds 收藏的板块ID集合
     * @property toastMessage 提示信息
     * @property currentSourceId 当前选中的数据源
     */
    data class ForumCategoryUiState(
        val categoriesState: UiStateWrapper<List<ForumGroupUiState>> = UiStateWrapper.Loading,
        val expandedGroupId: String? = null,
        val currentForum: Forum? = null,
        val favoriteForumIds: Set<String> = emptySet(),
        val toastMessage: String? = null,
        val notice: ai.saniou.thread.domain.model.forum.Notice? = null,
        val currentSourceId: String = "",
        val availableSources: List<Source> = emptyList(),
        val isCurrentSourceInitialized: Boolean = true
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
         * @param groupId 分组 ID
         */
        data class ToggleCategory(val groupId: String) : Event

        /**
         * 选中一个板块
         * @param forum 选中的板块
         */
        data class SelectForum(val forum: Forum) : Event

        /**
         * 选中主页 (Dashboard)
         */
        object SelectHome : Event

        /**
         * 切换一个板块的收藏状态
         * @param forum 要切换的板块
         */
        data class ToggleFavorite(val forum: Forum) : Event

        /**
         * 提示信息已显示
         */
        object ToastShown : Event

        /**
         * 标记公告为已读
         */
        object MarkNoticeRead : Event

        /**
         * 切换数据源
         * @param sourceId 数据源 ID
         */
        data class SelectSource(val sourceId: String) : Event
    }
}
