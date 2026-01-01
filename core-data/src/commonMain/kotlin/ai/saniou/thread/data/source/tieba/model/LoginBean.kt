package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- LoginBean ---

@Serializable
data class LoginBean(
    val anti: Anti,
    val ctime: Int,
    @SerialName("error_code")
    val errorCode: String,
    val logid: Long,
    @SerialName("server_time")
    val serverTime: String,
    val time: Long,
    val user: User
) {
    @Serializable
    data class Anti(
        val tbs: String
    )

    @Serializable
    data class User(
        val id: String,
        val name: String,
        val portrait: String
    )
}
