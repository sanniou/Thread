package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscoursePost(
    val id: Long,
    val name: String?,
    val username: String,
    @SerialName("avatar_template")
    val avatarTemplate: String,
    @SerialName("cooked")
    val cooked: String, // HTML content
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("post_number")
    val postNumber: Int,
    @SerialName("reply_to_post_number")
    val replyToPostNumber: Int? = null,
)
