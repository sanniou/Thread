package ai.saniou.forum.workflow.topicdetail

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.model.forum.Comment
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 定义 TopicDetail 功能模块的 UI 状态和事件
 */
interface TopicDetailContract {
    /**
     * UI 状态，承载页面的所有数据和状态
     *
     * @property topicWrapper 主楼内容状态
     * @property subCommentsWrapper 楼中楼列表状态
     * @property showSubCommentsDialog 是否显示楼中楼弹窗
     * @property activeCommentId 当前查看楼中楼的评论ID
     * @property replies 回复列表的分页数据流
     * @property totalPages 总页数
     * @property isSubscribed 是否已订阅
     * @property forumName 板块名称
     * @property lastReadCommentId 最后阅读的评论ID
     * @property isPoOnlyMode 是否开启“只看PO”模式
     * @property isTogglingSubscription 是否正在切换订阅状态
     */
    data class State(
        val topicWrapper: UiStateWrapper<Topic> = UiStateWrapper.Loading,
        val subCommentsWrapper: UiStateWrapper<List<Comment>> = UiStateWrapper.Loading,
        val showSubCommentsDialog: Boolean = false,
        val activeCommentId: String? = null,
        val replies: Flow<PagingData<Comment>> = emptyFlow(),
        val totalPages: Int = 1,
        val isSubscribed: Boolean = false,
        val forumName: String = "",
        val lastReadCommentId: String? = null,
        val isPoOnlyMode: Boolean = false,
        val isTogglingSubscription: Boolean = false,
    )

    /**
     * UI 事件，代表所有用户交互或生命周期事件
     */
    sealed interface Event {
        /**
         * 刷新当前帖子
         */
        object Refresh : Event

        /**
         * 跳转到指定页码
         * @param page 目标页码
         */
        data class JumpToPage(val page: Int) : Event

        /**
         * 切换“只看PO”模式
         */
        object TogglePoOnlyMode : Event

        /**
         * 切换订阅状态
         */
        object ToggleSubscription : Event

        /**
         * 复制帖子链接
         */
        object CopyLink : Event

        /**
         * 更新最后阅读的回复ID
         * @param id 回复 ID
         */
        data class UpdateLastReadReplyId(val id: String) : Event

        /**
         * 显示图片预览
         * @param imgPath 初始图片路径
         */
        data class ShowImagePreview(val imgPath: String) : Event

        /**
         * 加载更多图片
         */
        object LoadMoreImages : Event

        /**
         * 复制内容到剪贴板
         * @param content 要复制的内容
         */
        data class CopyContent(val content: String) : Event

        /**
         * 收藏主楼
         * @param metadata 帖子元数据
         */
        data class BookmarkTopic(val metadata: TopicMetadata) : Event

        /**
         * 收藏回复
         * @param reply 回复
         */
        data class BookmarkReply(val reply: Comment) : Event

        /**
         * 收藏图片
         * @param image 图片
         */
        data class BookmarkImage(val image: Image) : Event

        data class ShowSubComments(val commentId: String) : Event
        object HideSubComments : Event
        object RetryTopicLoad : Event
        object RetrySubCommentsLoad : Event
    }

    /**
     * 单次副作用事件，用于处理如 Snackbar、Toast、导航等一次性操作
     */
    sealed interface Effect {
        /**
         * 显示 Snackbar 消息
         * @param message 要显示的消息
         */
        data class ShowSnackbar(val message: String) : Effect

        /**
         * 将文本复制到剪贴板
         * @param text 要复制的文本
         */
        data class CopyToClipboard(val text: String) : Effect

        /**
         * 导航到图片预览页面
         */
        object NavigateToImagePreview : Effect
        data class ShowSubComments(val commentId: String) : Event
        object HideSubComments : Event
        object RetryTopicLoad : Event
        object RetrySubCommentsLoad : Event
    }
}
