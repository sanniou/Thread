package ai.saniou.thread.data.source.tieba.model

import kotlinx.serialization.Serializable

@Serializable
data class WebUploadPicBean(
    val errorMsg: String,
    val imageBaseSrc: String,
    val imageInfo: String,
    val imageSrc: String
)
