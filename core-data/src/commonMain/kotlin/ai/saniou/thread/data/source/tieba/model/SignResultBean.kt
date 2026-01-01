package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- SignResultBean ---

@Serializable
data class SignResultBean(
    @SerialName("user_info")
    val userInfo: UserInfo? = null,
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    val time: Long? = null
) {
    @Serializable
    data class UserInfo(
        @SerialName("user_id")
        val userId: String? = null,
        @SerialName("is_sign_in")
        val isSignIn: String? = null,
        @SerialName("cont_sign_num")
        val contSignNum: String? = null,
        @SerialName("user_sign_rank")
        val userSignRank: String? = null,
        @SerialName("sign_time")
        val signTime: String? = null,
        @SerialName("sign_bonus_point")
        val signBonusPoint: String? = null,
        @SerialName("level_name")
        val levelName: String? = null,
        @SerialName("levelup_score")
        val levelUpScore: String? = null,
        @SerialName("all_level_info")
        val allLevelInfo: List<AllLevelInfo> = emptyList()
    ) {
        @Serializable
        data class AllLevelInfo(
            val id: String,
            val name: String,
            val score: String
        )
    }
}
