package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebReplyResultBean(
    @SerialName("no")
    val errorCode: Int,
    @SerialName("error")
    val errorMsg: String,
    val data: WebReplyDataBean
) {
    @Serializable
    data class WebReplyDataBean(
        @SerialName("is_not_top_stick")
        val isNotTopStick: Int,
        val pid: Long,
        val tid: Long
    )
}
