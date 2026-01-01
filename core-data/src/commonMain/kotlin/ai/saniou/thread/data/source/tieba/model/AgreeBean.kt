package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- AgreeBean ---

@Serializable
data class AgreeBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    val data: AgreeDataBean? = null
) {
    @Serializable
    data class AgreeDataBean(
        val agree: AgreeInfoBean? = null
    )

    @Serializable
    data class AgreeInfoBean(
        val score: String? = null
    )
}
