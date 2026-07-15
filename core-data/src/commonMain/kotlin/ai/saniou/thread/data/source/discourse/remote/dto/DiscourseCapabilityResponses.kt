package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseSearchResponse(
    val posts: List<DiscourseSearchPost> = emptyList(),
    val topics: List<DiscourseTopic> = emptyList(),
    val users: List<DiscourseUser> = emptyList(),
)

@Serializable
data class DiscourseSearchPost(
    val id: Long,
    val name: String? = null,
    val username: String,
    @SerialName("avatar_template") val avatarTemplate: String = "",
    val cooked: String = "",
    @SerialName("blurb") val blurb: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("post_number") val postNumber: Int = 0,
    @SerialName("topic_id") val topicId: Long,
)

@Serializable
data class DiscourseUserActionsResponse(
    @SerialName("user_actions") val userActions: List<DiscourseUserAction> = emptyList(),
)

@Serializable
data class DiscourseUserAction(
    val excerpt: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("avatar_template") val avatarTemplate: String = "",
    val slug: String = "",
    @SerialName("topic_id") val topicId: Long,
    @SerialName("post_number") val postNumber: Int = 1,
    @SerialName("post_id") val postId: Long? = null,
    val title: String = "",
    val username: String,
    val name: String? = null,
)

@Serializable
data class DiscourseCreatePostResponse(
    val id: Long,
    @SerialName("topic_id") val topicId: Long,
    @SerialName("post_number") val postNumber: Int,
)
