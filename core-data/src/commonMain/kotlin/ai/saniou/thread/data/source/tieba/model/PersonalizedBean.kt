package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- PersonalizedBean ---

@Serializable
data class PersonalizedBean(
    @SerialName("error_code")
    val errorCode: String? = null,
    @SerialName("error_msg")
    val errorMsg: String? = null,
    @SerialName("thread_list")
    val threadList: List<ThreadBean>? = null,
    @SerialName("thread_personalized")
    val threadPersonalized: List<ThreadPersonalizedBean>? = null
) {
    @Serializable
    data class ThreadPersonalizedBean(
        val tid: String? = null,
        @SerialName("dislike_resource")
        val dislikeResource: List<DislikeResourceBean>? = null
    )

    @Serializable
    data class DislikeResourceBean(
        val extra: String? = null,
        @SerialName("dislike_id")
        val dislikeId: String? = null,
        @SerialName("dislike_reason")
        val dislikeReason: String? = null
    )

    @Serializable
    data class ThreadBean(
        val id: String? = null,
        val tid: String? = null,
        val title: String? = null,
        val author: AuthorBean? = null,
        @SerialName("reply_num")
        val replyNum: String? = null,
        @SerialName("share_num")
        val shareNum: String? = null,
        @SerialName("view_num")
        val viewNum: String? = null,
        @SerialName("last_time")
        val lastTime: String? = null,
        @SerialName("last_time_int")
        val lastTimeInt: String? = null,
        @SerialName("agree_num")
        val agreeNum: String? = null,
        @SerialName("is_top")
        val isTop: String? = null,
        @SerialName("is_good")
        val isGood: String? = null,
        @SerialName("is_ntitle")
        val isNoTitle: String? = null,
        @SerialName("fid")
        val forumId: String? = null,
        @SerialName("fname")
        val forumName: String? = null,
        @SerialName("video_info")
        val videoInfo: VideoInfoBean? = null,
        val media: List<MediaInfoBean>? = null,
        @SerialName("abstract")
        val abstractBeans: List<AbstractBean>? = null
    )

    @Serializable
    data class AuthorBean(
        val id: String? = null,
        val name: String? = null,
        @SerialName("name_show")
        val nameShow: String? = null,
        val portrait: String? = null,
        @SerialName("has_concerned")
        val hasConcerned: String? = null
    )

    @Serializable
    data class VideoInfoBean(
        @SerialName("video_url")
        val videoUrl: String? = null,
        @SerialName("thumbnail_url")
        val thumbnailUrl: String? = null,
        @SerialName("origin_video_url")
        val originVideoUrl: String? = null
    )

    @Serializable
    data class MediaInfoBean(
        val type: String? = null,
        @SerialName("show_original_btn")
        val showOriginalBtn: String? = null,
        @SerialName("is_long_pic")
        val isLongPic: String? = null,
        @SerialName("is_gif")
        val isGif: String? = null,
        @SerialName("big_pic")
        val bigPic: String? = null,
        @SerialName("dynamic_pic")
        val dynamicPic: String? = null,
        @SerialName("src_pic")
        val srcPic: String? = null,
        @SerialName("post_id")
        val postId: String? = null,
        @SerialName("origin_pic")
        val originPic: String? = null
    )

    @Serializable
    data class AbstractBean(
        val type: String,
        val text: String
    )
}
