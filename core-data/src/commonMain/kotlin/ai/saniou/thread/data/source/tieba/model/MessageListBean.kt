package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- MessageListBean ---

@Serializable
data class MessageListBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    val time: Long = 0,
    @SerialName("reply_list")
    val replyList: List<MessageInfoBean>? = null,
    @SerialName("at_list")
    val atList: List<MessageInfoBean>? = null,
    val page: PageInfoBean? = null,
    val message: MsgBean.MessageBean? = null
) {
    @Serializable
    data class UserInfoBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        val portrait: String? = null
    )

    @Serializable
    data class ReplyerInfoBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        val portrait: String? = null,
        @SerialName("is_friend")
        val isFriend: String? = null,
        @SerialName("is_fans")
        val isFans: String? = null
    )

    @Serializable
    data class MessageInfoBean(
        @SerialName("is_floor")
        val isFloor: String? = null,
        val title: String? = null,
        val content: String? = null,
        @SerialName("quote_content")
        val quoteContent: String? = null,
        val replyer: ReplyerInfoBean? = null,
        @SerialName("quote_user")
        val quoteUser: UserInfoBean? = null,
        @SerialName("thread_id")
        val threadId: String? = null,
        @SerialName("post_id")
        val postId: String? = null,
        val time: String? = null,
        @SerialName("fname")
        val forumName: String? = null,
        @SerialName("quote_pid")
        val quotePid: String? = null,
        @SerialName("thread_type")
        val threadType: String? = null,
        val unread: String? = null
    )

    @Serializable
    data class PageInfoBean(
        @SerialName("current_page")
        val currentPage: String? = null,
        @SerialName("has_more")
        val hasMore: String? = null,
        @SerialName("has_prev")
        val hasPrev: String? = null
    )
}
