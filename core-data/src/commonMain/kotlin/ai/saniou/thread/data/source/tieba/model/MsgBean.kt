package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- MsgBean ---

@Serializable
data class MsgBean(
    val message: MessageBean? = null,
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null
) {
    @Serializable
    data class MessageBean(
        @SerialName("replyme")
        val replyMe: String? = null,
        @SerialName("atme")
        val atMe: String? = null,
        val fans: String? = null
    )
}
