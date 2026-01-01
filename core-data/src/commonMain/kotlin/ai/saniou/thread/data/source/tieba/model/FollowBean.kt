package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- FollowBean ---

@Serializable
data class FollowBean(
    @SerialName("error_code")
    val errorCode: Int,
    @SerialName("error_msg")
    val errorMsg: String,
    val status: String,
    val info: Info
) {
    @Serializable
    data class Info(
        @SerialName("toast_text")
        val toastText: String,
        @SerialName("is_toast")
        val isToast: String
    )
}
