package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebProfile(
    @SerialName("is_login")
    val isLogin: Int,
    val sid: String,
    val user: WebUser
) {
    @Serializable
    data class WebUser(
        val intro: String,
        val name: String,
        @SerialName("name_show")
        val nameShow: String,
        val portrait: String,
        val sex: Int,
        @SerialName("show_nickname")
        val showNickName: String
    )
}
