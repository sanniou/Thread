package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyInfoBean(
    @SerialName("itb_tbs")
    val itbTbs: String,
    val tbs: String,
    @SerialName("portrait_url")
    val avatarUrl: String,
    val uid: Long,
    @SerialName("user_sex")
    val userSex: Int,
    @SerialName("name_show")
    val showName: String,
    val intro: String,
    val name: String,
    @SerialName("concern_num")
    val concernNum: String,
    @SerialName("fans_num")
    val fansNum: String,
    @SerialName("like_forum_num")
    val likeForumNum: String,
    @SerialName("post_num")
    val postNum: String,
    @SerialName("is_login")
    val isLogin: Boolean
)
