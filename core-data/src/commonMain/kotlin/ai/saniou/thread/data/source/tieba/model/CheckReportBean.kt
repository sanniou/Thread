package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- CheckReportBean ---

@Serializable
data class CheckReportBean(
    @SerialName("errno")
    val errorCode: Int?,
    @SerialName("errmsg")
    val errorMsg: String?,
    val data: CheckReportDataBean
) {
    @Serializable
    data class CheckReportDataBean(
        val url: String = ""
    )
}
