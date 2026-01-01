package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- SearchThreadBean ---

@Serializable
data class SearchThreadBean(
    @SerialName("no")
    val errorCode: Int,
    @SerialName("error")
    val errorMsg: String,
    val data: DataBean
) {
    @Serializable
    data class DataBean(
        @SerialName("has_more")
        val hasMore: Int,
        @SerialName("current_page")
        val currentPage: Int,
        @SerialName("post_list")
        val postList: List<ThreadInfoBean> = emptyList()
    )

    @Serializable
    data class ThreadInfoBean(
        val tid: String,
        val pid: String,
        val cid: String = "0",
        val title: String,
        val content: String,
        val time: String,
        @SerialName("modified_time")
        val modifiedTime: Long,
        @SerialName("post_num")
        val postNum: String,
        @SerialName("like_num")
        val likeNum: String,
        @SerialName("share_num")
        val shareNum: String,
        @SerialName("forum_id")
        val forumId: String,
        @SerialName("forum_name")
        val forumName: String,
        val user: UserInfoBean,
        val type: Int,
        @SerialName("forum_info")
        val forumInfo: ForumInfo,
        val media: List<MediaInfo> = emptyList(),
        @SerialName("main_post")
        val mainPost: MainPost? = null,
        @SerialName("post_info")
        val postInfo: PostInfo? = null
    )

    @Serializable
    data class MediaInfo(
        val type: String,
        val size: String? = null,
        val width: String,
        val height: String,
        @SerialName("water_pic")
        val waterPic: String? = null,
        @SerialName("small_pic")
        val smallPic: String? = null,
        @SerialName("big_pic")
        val bigPic: String? = null,
        val src: String? = null,
        val vsrc: String? = null,
        val vhsrc: String? = null,
        val vpic: String? = null
    )

    @Serializable
    data class MainPost(
        val title: String,
        val content: String,
        val tid: Long,
        val user: UserInfoBean,
        @SerialName("like_num")
        val likeNum: String,
        @SerialName("share_num")
        val shareNum: String,
        @SerialName("post_num")
        val postNum: String
    )

    @Serializable
    data class PostInfo(
        val tid: Long,
        val pid: Long,
        val title: String,
        val content: String,
        val user: UserInfoBean
    )

    @Serializable
    data class ForumInfo(
        @SerialName("forum_name")
        val forumName: String,
        val avatar: String
    )

    @Serializable
    data class UserInfoBean(
        @SerialName("user_name")
        val userName: String?,
        @SerialName("show_nickname")
        val showNickname: String?,
        @SerialName("user_id")
        val userId: String,
        val portrait: String?
    )
}
