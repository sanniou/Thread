package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- MSignBean ---

@Serializable
data class MSignBean(
    val ctime: Int,
    val error: Error,
    @SerialName("error_code")
    val errorCode: String,
    val info: List<Info>,
    @SerialName("is_timeout")
    val isTimeout: String,
    val logid: Long,
    @SerialName("server_time")
    val serverTime: String,
    @SerialName("show_dialog")
    val showDialog: String,
    @SerialName("sign_notice")
    val signNotice: String,
    val time: Int,
    @SerialName("timeout_notice")
    val timeoutNotice: String
) {
    @Serializable
    data class Error(
        val errmsg: String,
        val errno: String,
        val usermsg: String
    )

    @Serializable
    data class Info(
        @SerialName("cur_score")
        val curScore: String,
        val error: Error,
        @SerialName("forum_id")
        val forumId: String,
        @SerialName("forum_name")
        val forumName: String,
        @SerialName("is_filter")
        val isFilter: String,
        @SerialName("is_on")
        val isOn: String,
        @SerialName("sign_day_count")
        val signDayCount: String,
        val signed: String
    ) {
        @Serializable
        data class Error(
            @SerialName("err_no")
            val errNo: String,
            val errmsg: String,
            val usermsg: String
        )
    }
}
