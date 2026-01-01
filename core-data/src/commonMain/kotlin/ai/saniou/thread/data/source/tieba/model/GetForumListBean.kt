package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- GetForumListBean ---

@Serializable
data class GetForumListBean(
    @SerialName("button_content")
    val buttonContent: String,
    @SerialName("can_use")
    val canUse: String,
    val content: String,
    val error: Error,
    @SerialName("error_code")
    val errorCode: String,
    @SerialName("forum_info")
    val forumInfo: List<ForumInfo>,
    val level: String,
    @SerialName("msign_step_num")
    val msignStepNum: String,
    @SerialName("num_notice")
    val numNotice: String,
    @SerialName("server_time")
    val serverTime: String,
    @SerialName("show_dialog")
    val showDialog: String,
    @SerialName("sign_max_num")
    val signMaxNum: String,
    @SerialName("sign_new")
    val signNew: String,
    @SerialName("sign_notice")
    val signNotice: String,
    @SerialName("text_color")
    val textColor: String,
    @SerialName("text_mid")
    val textMid: String,
    @SerialName("text_pre")
    val textPre: String,
    @SerialName("text_suf")
    val textSuf: String,
    val time: Int,
    val title: String,
    val user: User,
    val valid: String
) {
    @Serializable
    data class Error(
        val errmsg: String,
        val errno: String,
        val usermsg: String
    )

    @Serializable
    data class ForumInfo(
        val avatar: String,
        @SerialName("cont_sign_num")
        val contSignNum: String,
        @SerialName("forum_id")
        val forumId: String,
        @SerialName("forum_name")
        val forumName: String,
        @SerialName("is_sign_in")
        val isSignIn: String,
        @SerialName("need_exp")
        val needExp: String,
        @SerialName("user_exp")
        val userExp: String,
        @SerialName("user_level")
        val userLevel: String
    )

    @Serializable
    data class User(
        @SerialName("pay_member_info")
        val payMemberInfo: PayMemberInfo,
        @SerialName("unsign_info")
        val unsignInfo: List<UnsignInfo>
    ) {
        @Serializable
        data class PayMemberInfo(
            @SerialName("end_time")
            val endTime: String,
            @SerialName("pic_url")
            val picUrl: String,
            @SerialName("props_id")
            val propsId: String
        )

        @Serializable
        data class UnsignInfo(
            val level: String,
            val num: String
        )
    }
}
