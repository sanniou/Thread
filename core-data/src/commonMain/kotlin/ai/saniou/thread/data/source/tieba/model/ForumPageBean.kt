package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- ForumPageBean ---

@Serializable
data class ForumPageBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    val forum: ForumBean? = null,
    val anti: AntiBean? = null,
    val user: UserBean? = null,
    val page: PageBean? = null,
    @SerialName("thread_list")
    val threadList: List<ThreadBean>? = null,
    @SerialName("user_list")
    val userList: List<UserBean>? = null
) {
    @Serializable
    data class ForumBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("is_like")
        val isLike: String? = null,
        @SerialName("user_level")
        val userLevel: String? = null,
        @SerialName("level_id")
        val levelId: String? = null,
        @SerialName("level_name")
        val levelName: String? = null,
        @SerialName("is_exists")
        val isExists: String? = null,
        @SerialName("cur_score")
        val curScore: String? = null,
        @SerialName("levelup_score")
        val levelUpScore: String? = null,
        @SerialName("member_num")
        val memberNum: String? = null,
        @SerialName("thread_num")
        val threadNum: String? = null,
        @SerialName("theme_color")
        val themeColor: ThemeColors? = null,
        @SerialName("post_num")
        val postNum: String? = null,
        val managers: List<ManagerBean>? = null,
        val zyqTitle: String? = null,
        val zyqDefine: List<ZyqDefineBean>? = null,
        val zyqFriend: List<String>? = null,
        @SerialName("good_classify")
        val goodClassify: List<GoodClassifyBean>? = null,
        val slogan: String? = null,
        val avatar: String? = null,
        val tids: String? = null,
        @SerialName("sign_in_info")
        val signInInfo: SignInInfo? = null
    ) {
        @Serializable
        data class SignInInfo(
            @SerialName("user_info")
            val userInfo: UserInfo? = null
        ) {
            @Serializable
            data class UserInfo(
                @SerialName("is_sign_in")
                val isSignIn: String? = null
            )
        }
    }

    @Serializable
    data class ThemeColors(
        val day: ThemeColor,
        val dark: ThemeColor,
        val night: ThemeColor
    ) {
        @Serializable
        data class ThemeColor(
            @SerialName("common_color")
            val commonColor: String,
            @SerialName("dark_color")
            val darkColor: String,
            @SerialName("font_color")
            val fontColor: String,
            @SerialName("light_color")
            val lightColor: String,
        )
    }

    @Serializable
    data class ManagerBean(
        val id: String? = null,
        val name: String? = null
    )

    @Serializable
    data class ZyqDefineBean(
        val name: String? = null,
        val link: String? = null
    )

    @Serializable
    data class GoodClassifyBean(
        @SerialName("class_id")
        val classId: String? = null,
        @SerialName("class_name")
        val className: String? = null
    )

    @Serializable
    data class AntiBean(
        val tbs: String? = null,
        @SerialName("ifpost")
        val ifPost: String? = null,
        @SerialName("forbid_flag")
        val forbidFlag: String? = null,
        @SerialName("forbid_info")
        val forbidInfo: String? = null
    )

    @Serializable
    data class UserBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        val portrait: String? = null
    )

    @Serializable
    data class PageBean(
        @SerialName("page_size")
        val pageSize: String? = null,
        val offset: String? = null,
        @SerialName("current_page")
        val currentPage: String? = null,
        @SerialName("total_count")
        val totalCount: String? = null,
        @SerialName("total_page")
        val totalPage: String? = null,
        @SerialName("has_more")
        val hasMore: String? = null,
        @SerialName("has_prev")
        val hasPrev: String? = null,
        @SerialName("cur_good_id")
        val curGoodId: String? = null
    )

    @Serializable
    data class ThreadBean(
        val id: String? = null,
        val tid: String? = null,
        val title: String? = null,
        @SerialName("reply_num")
        val replyNum: String? = null,
        @SerialName("view_num")
        val viewNum: String? = null,
        @SerialName("last_time")
        val lastTime: String? = null,
        @SerialName("last_time_int")
        val lastTimeInt: String? = null,
        @SerialName("create_time")
        val createTime: String? = null,
        @SerialName("agree_num")
        val agreeNum: String? = null,
        @SerialName("is_top")
        val isTop: String? = null,
        @SerialName("is_good")
        val isGood: String? = null,
        @SerialName("is_ntitle")
        val isNoTitle: String? = null,
        @SerialName("author_id")
        val authorId: String? = null,
        @SerialName("video_info")
        val videoInfo: PersonalizedBean.VideoInfoBean? = null,
        val media: List<PersonalizedBean.MediaInfoBean>? = null,
        @SerialName("abstract")
        val abstractBeans: List<PersonalizedBean.AbstractBean?>? = null
    )
}
