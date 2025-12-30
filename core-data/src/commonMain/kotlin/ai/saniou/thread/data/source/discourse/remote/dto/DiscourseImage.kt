package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseImage(
    val id: Long? = null,
    @SerialName("max_width")
    val maxWidth: Int? = null,
    @SerialName("max_height")
    val maxHeight: Int? = null,
    val width: Int,
    val height: Int,
    val url: String,
)
