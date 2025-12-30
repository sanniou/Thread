package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseLastPoster(
    val id: Long,
    val username: String,
    val name: String?,
    @SerialName("avatar_template")
    val avatarTemplate: String,
    @SerialName("animated_avatar")
    val animatedAvatar: String? = null
)
