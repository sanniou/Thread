package ai.saniou.thread.data.source.tieba.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Common Base ---

@Serializable
open class BaseBean

@Serializable
open class ErrorBean : BaseBean() {
    @SerialName("error_code")
    val errorCode: String? = null

    @SerialName("error_msg")
    val errorMsg: String? = null
}
