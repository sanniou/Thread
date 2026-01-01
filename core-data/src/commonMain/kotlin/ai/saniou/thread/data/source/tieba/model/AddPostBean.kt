package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- AddPostBean ---

@Serializable
data class AddPostBean(
    @SerialName("anti_stat")
    val antiStat: AntiStat = AntiStat(),
    @SerialName("contri_info")
    val contriInfo: List<String> = listOf(), // Any -> String placeholder, check usage
    val ctime: Int = 0,
    @SerialName("error_code")
    val errorCode: String = "",
    val exp: Exp = Exp(),
    val info: Info = Info(),
    val logid: Long = 0,
    val msg: String = "",
    val opgroup: String = "",
    val pid: String = "",
    @SerialName("pre_msg")
    val preMsg: String = "",
    @SerialName("server_time")
    val serverTime: String = "",
    val tid: String = "",
    val time: Int = 0
) {
    @Serializable
    data class AntiStat(
        @SerialName("block_stat")
        val blockStat: String = "",
        @SerialName("days_tofree")
        val daysTofree: String = "",
        @SerialName("has_chance")
        val hasChance: String = "",
        @SerialName("hide_stat")
        val hideStat: String = "",
        @SerialName("vcode_stat")
        val vcodeStat: String = ""
    )

    @Serializable
    data class Exp(
        @SerialName("color_msg")
        val colorMsg: String = "",
        @SerialName("current_level")
        val currentLevel: String = "",
        @SerialName("current_level_max_exp")
        val currentLevelMaxExp: String = "",
        val old: String = "",
        @SerialName("pre_msg")
        val preMsg: String = ""
    )

    @Serializable
    data class Info(
        @SerialName("access_state")
        val accessState: List<String> = listOf(),
        @SerialName("confilter_hitwords")
        val confilterHitwords: List<String> = listOf(),
        @SerialName("need_vcode")
        val needVcode: String = "",
        @SerialName("pass_token")
        val passToken: String = "",
        @SerialName("vcode_md5")
        val vcodeMd5: String = "",
        @SerialName("vcode_prev_type")
        val vcodePrevType: String = "",
        @SerialName("vcode_type")
        val vcodeType: String = ""
    )
}
