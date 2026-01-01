package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PicPageBean(
    @SerialName("error_code")
    val errorCode: String,
    val forum: ForumBean,
    @SerialName("pic_amount")
    val picAmount: String,
    @SerialName("pic_list")
    val picList: List<PicBean>
) {
    @Serializable
    data class ForumBean(
        val name: String,
        val id: String
    )

    @Serializable
    data class PicBean(
        @SerialName("overall_index")
        val overAllIndex: String,
        @SerialName("is_long_pic")
        val isLongPic: Boolean,
        @SerialName("show_original_btn")
        val showOriginalBtn: Boolean,
        @SerialName("is_blocked_pic")
        val isBlockedPic: Boolean,
        val img: ImgBean,
        @SerialName("post_id")
        val postId: String?,
        @SerialName("user_id")
        val userId: String?,
        @SerialName("user_name")
        val userName: String?
    )

    @Serializable
    data class ImgBean(
        val original: ImgInfoBean,
        val medium: ImgInfoBean?,
        val screen: ImgInfoBean?
    )

    @Serializable
    data class ImgInfoBean(
        val id: String,
        val width: String?,
        val height: String?,
        val size: String,
        val format: String,
        @SerialName("waterurl")
        val waterUrl: String,
        @SerialName("big_cdn_src")
        val bigCdnSrc: String,
        val url: String,
        @SerialName("original_src")
        val originalSrc: String
    )
}
