package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- UploadPictureResultBean ---

@Serializable
data class UploadPictureResultBean(
    @SerialName("error_code")
    val errorCode: String,
    @SerialName("error_msg")
    val errorMsg: String,
    val resourceId: String,
    val chunkNo: String,
    val picId: String,
    val picInfo: PicInfo
) {
    @Serializable
    data class PicInfo(
        val originPic: PicInfoItem,
        val bigPic: PicInfoItem,
        val smallPic: PicInfoItem
    )

    @Serializable
    data class PicInfoItem(
        val width: String,
        val height: String,
        val type: String,
        val picUrl: String
    )
}
