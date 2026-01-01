package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- InitNickNameBean ---

@Serializable
data class InitNickNameBean(
    val ctime: Int,
    @SerialName("error_code")
    val errorCode: String,
    val logid: Long,
    @SerialName("server_time")
    val serverTime: String,
    val switch: List<Switch>,
    val time: Long,
    @SerialName("user_info")
    val userInfo: UserInfo
) {
    @Serializable
    data class Switch(
        val name: String,
        val type: String
    )

    @Serializable
    data class UserInfo(
        @SerialName("name_show")
        val nameShow: String,
        @SerialName("tieba_uid")
        val tiebaUid: String,
        @SerialName("user_name")
        val userName: String,
        @SerialName("user_nickname")
        val userNickname: String
    )
}
