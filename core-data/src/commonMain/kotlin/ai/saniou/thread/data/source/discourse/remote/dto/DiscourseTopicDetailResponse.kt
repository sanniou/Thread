package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseTopicDetailResponse(
    val id: Long,
    val title: String,
    @SerialName("fancy_title")
    val fancyTitle: String,
    @SerialName("posts_count")
    val postsCount: Int,
    @SerialName("reply_count")
    val replyCount: Int,
    @SerialName("post_stream")
    val postStream: DiscoursePostStream,
    @SerialName("category_id")
    val categoryId: Long
)
