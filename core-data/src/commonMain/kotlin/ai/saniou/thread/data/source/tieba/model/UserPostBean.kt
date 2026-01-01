package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- UserPostBean ---

@Serializable
data class UserPostBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    @SerialName("hide_post")
    val hidePost: String? = null,
    @SerialName("post_list")
    val postList: List<PostBean>? = null
) {
    @Serializable
    data class AgreeBean(
        @SerialName("agree_num")
        val agreeNum: String? = null,
        @SerialName("disagree_num")
        val disagreeNum: String? = null,
        @SerialName("diff_agree_num")
        val diffAgreeNum: String? = null,
        @SerialName("has_agree")
        val hasAgree: String? = null
    )

    @Serializable
    data class PostBean(
        val agree: AgreeBean? = null,
        @SerialName("forum_id")
        val forumId: String? = null,
        @SerialName("thread_id")
        val threadId: String? = null,
        @SerialName("post_id")
        val postId: String? = null,
        @SerialName("is_thread")
        val isThread: String? = null,
        @SerialName("create_time")
        val createTime: String? = null,
        @SerialName("is_ntitle")
        val isNoTitle: String? = null,
        @SerialName("forum_name")
        val forumName: String? = null,
        val title: String? = null,
        @SerialName("user_name")
        val userName: String? = null,
        @SerialName("is_post_deleted")
        val isPostDeleted: String? = null,
        @SerialName("reply_num")
        val replyNum: String? = null,
        @SerialName("freq_num")
        val freqNum: String? = null,
        @SerialName("user_id")
        val userId: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        @SerialName("user_portrait")
        val userPortrait: String? = null,
        @SerialName("post_type")
        val postType: String? = null,
        val content: List<ContentBean>? = null,
        @SerialName("abstract")
        val abstracts: List<PostContentBean>? = null
    )

    @Serializable
    data class ContentBean(
        @SerialName("post_content")
        val postContent: List<PostContentBean>? = null,
        @SerialName("create_time")
        val createTime: String? = null,
        @SerialName("post_id")
        val postId: String? = null
    )

    @Serializable
    data class PostContentBean(
        val type: String? = null,
        val text: String? = null
    )
}
