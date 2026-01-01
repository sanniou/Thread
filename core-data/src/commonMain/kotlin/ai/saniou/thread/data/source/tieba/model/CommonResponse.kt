package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- CommonResponse ---

@Serializable
data class CommonResponse(
    @SerialName("error_code")
    val errorCode: Int = 0,
    @SerialName("error_msg")
    val errorMsg: String = ""
)
