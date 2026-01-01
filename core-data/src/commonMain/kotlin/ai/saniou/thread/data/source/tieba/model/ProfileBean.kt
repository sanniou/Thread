package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- ProfileBean ---

@Serializable
data class ProfileBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    val anti: AntiBean? = null,
    val user: UserBean? = null
) {
    @Serializable
    data class AntiBean(
        val tbs: String? = null
    )

    @Serializable
    data class UserBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        val portrait: String? = null,
        val intro: String? = null,
        val sex: String? = null,
        @SerialName("post_num")
        val postNum: String? = null,
        @SerialName("repost_num")
        val repostNum: String? = null,
        @SerialName("thread_num")
        val threadNum: String? = null,
        @SerialName("tb_age")
        val tbAge: String? = null,
        @SerialName("my_like_num")
        val myLikeNum: String? = null,
        @SerialName("like_forum_num")
        val likeForumNum: String? = null,
        @SerialName("concern_num")
        val concernNum: String? = null,
        @SerialName("fans_num")
        val fansNum: String? = null,
        @SerialName("has_concerned")
        val hasConcerned: String? = null,
        @SerialName("is_fans")
        val isFans: String? = null
    )
}
