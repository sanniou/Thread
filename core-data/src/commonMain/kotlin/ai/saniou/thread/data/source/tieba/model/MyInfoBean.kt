package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyInfoBean(
    @SerialName("itb_tbs")
    val itbTbs: String = "",
    val tbs: String = "",
    @SerialName("portrait_url")
    val avatarUrl: String = "",
    val uid: Long = 0,
    @SerialName("user_sex")
    val userSex: Int = 0,
    @SerialName("name_show")
    val showName: String = "",
    val intro: String = "",
    val name: String = "",
    @SerialName("concern_num")
    val concernNum: String = "0",
    @SerialName("fans_num")
    val fansNum: String = "0",
    @SerialName("like_forum_num")
    val likeForumNum: String = "0",
    @SerialName("post_num")
    val postNum: String = "0",
    @SerialName("is_login")
    val isLogin: Boolean = false,
)
