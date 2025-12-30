package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseTopicPoster(
    val extras: String? = null,
    val description: String,
    @SerialName("user_id")
    val userId: Long,
    @SerialName("primary_group_id")
    val primaryGroupId: Long? = null
)
