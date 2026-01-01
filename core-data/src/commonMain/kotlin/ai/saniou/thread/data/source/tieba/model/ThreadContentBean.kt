package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- ThreadContentBean ---

@Serializable
data class ThreadContentBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    @SerialName("post_list")
    val postList: List<PostListItemBean>? = null,
    val page: PageInfoBean? = null,
    val user: UserInfoBean? = null,
    val forum: ForumInfoBean? = null,
    @SerialName("display_forum")
    val displayForum: ForumInfoBean? = null,
    @SerialName("has_floor")
    val hasFloor: String? = null,
    @SerialName("is_new_url")
    val isNewUrl: String? = null,
    @SerialName("user_list")
    val userList: List<UserInfoBean>? = null,
    val thread: ThreadBean? = null,
    val anti: AntiInfoBean? = null
) {
    @Serializable
    data class AntiInfoBean(
        val tbs: String? = null
    )

    @Serializable
    data class ThreadInfoBean(
        @SerialName("thread_id")
        val threadId: String? = null,
        @SerialName("first_post_id")
        val firstPostId: String? = null
    )

    @Serializable
    data class AgreeBean(
        @SerialName("agree_num")
        val agreeNum: String? = null,
        @SerialName("disagree_num")
        val disagreeNum: String? = null,
        @SerialName("diff_agree_num")
        val diffAgreeNum: String? = null,
        @SerialName("has_agree")
        val hasAgree: String? = null
    )

    @Serializable
    data class ThreadBean(
        val id: String? = null,
        val title: String? = null,
        @SerialName("thread_info")
        val threadInfo: ThreadInfoBean? = null,
        @SerialName("origin_thread_info")
        val originThreadInfo: OriginThreadInfo? = null,
        val author: UserInfoBean? = null,
        @SerialName("reply_num")
        val replyNum: String? = null,
        @SerialName("collect_status")
        val collectStatus: String? = null,
        @SerialName("agree_num")
        val agreeNum: String? = null,
        @SerialName("create_time")
        val createTime: String? = null,
        @SerialName("post_id")
        val postId: String? = null,
        @SerialName("thread_id")
        val threadId: String? = null,
        val agree: AgreeBean? = null
    )

    @Serializable
    data class UserInfoBean(
        @SerialName("is_login")
        val isLogin: String? = null,
        val id: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        val portrait: String? = null,
        val type: String? = null,
        @SerialName("level_id")
        val levelId: String? = null,
        @SerialName("is_like")
        val isLike: String? = null,
        @SerialName("is_bawu")
        val isBawu: String? = null,
        @SerialName("bawu_type")
        val bawuType: String? = null,
        @SerialName("ip_address")
        val ipAddress: String? = null
    )

    @Serializable
    data class ForumInfoBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("is_exists")
        val isExists: String? = null,
        val avatar: String? = null,
        @SerialName("first_class")
        val firstClass: String? = null,
        @SerialName("second_class")
        val secondClass: String? = null,
        @SerialName("is_liked")
        val isLiked: String? = null,
        @SerialName("is_brand_forum")
        val isBrandForum: String? = null
    )

    @Serializable
    data class PageInfoBean(
        val offset: String? = null,
        @SerialName("current_page")
        val currentPage: String? = null,
        @SerialName("total_page")
        val totalPage: String? = null,
        @SerialName("has_more")
        val hasMore: String? = null,
        @SerialName("has_prev")
        val hasPrev: String? = null
    )

    @Serializable
    data class OriginThreadInfo(
        val title: String? = null,
        val content: List<ContentBean>? = null
    )

    @Serializable
    data class PostListItemBean(
        val id: String? = null,
        val title: String? = null,
        val floor: String? = null,
        val time: String? = null,
        val content: List<ContentBean>? = null,
        val agree: AgreeBean? = null,
        @SerialName("author_id")
        val authorId: String? = null,
        val author: UserInfoBean? = null,
        @SerialName("sub_post_number")
        val subPostNumber: String? = null,
        @SerialName("sub_post_list")
        val subPostList: SubPostListBean? = null
    )

    @Serializable
    data class SubPostListBean(
        val pid: String? = null,
        @SerialName("sub_post_list")
        val subPostList: List<PostListItemBean>? = null
    )

    @Serializable
    data class ContentBean(
        val type: String? = null,
        val text: String? = null,
        val link: String? = null,
        val src: String? = null,
        val uid: String? = null,
        @SerialName("origin_src")
        val originSrc: String? = null,
        @SerialName("cdn_src")
        val cdnSrc: String? = null,
        @SerialName("cdn_src_active")
        val cdnSrcActive: String? = null,
        @SerialName("big_cdn_src")
        val bigCdnSrc: String? = null,
        @SerialName("during_time")
        val duringTime: String? = null,
        val bsize: String? = null,
        val c: String? = null,
        val width: String? = null,
        val height: String? = null,
        @SerialName("is_long_pic")
        val isLongPic: String? = null,
        @SerialName("voice_md5")
        val voiceMD5: String? = null
    )
}
