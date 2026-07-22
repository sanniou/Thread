package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- LikeForumResultBean ---

@Serializable
data class LikeForumResultBean(
    @SerialName("error_code")
    val errorCode: String = "0",
    val error: ErrorInfo? = null,
    val info: Info? = null,
    val userPerm: UserPermInfo? = null,
) {
    @Serializable
    data class UserPermInfo(
        @SerialName("level_id")
        val levelId: String = "",
        @SerialName("level_name")
        val levelName: String = "",
    )

    @Serializable
    data class Info(
        @SerialName("cur_score")
        val curScore: String = "",
        @SerialName("levelup_score")
        val levelUpScore: String = "",
        @SerialName("level_id")
        val levelId: String = "",
        @SerialName("level_name")
        val levelName: String = "",
        @SerialName("member_sum")
        val memberSum: String = "",
    )

    @Serializable
    data class ErrorInfo(
        val errno: String = "",
        val errmsg: String = "",
        val usermsg: String = "",
    )
}
