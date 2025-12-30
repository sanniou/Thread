package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseCategoryTopic(
    @SerialName("fancy_title")
    val fancyTitle: String,
    val id: Long,
    val title: String,
    val slug: String,
    @SerialName("posts_count")
    val postsCount: Int,
    @SerialName("reply_count")
    val replyCount: Int,
    @SerialName("highest_post_number")
    val highestPostNumber: Int,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_posted_at")
    val lastPostedAt: String,
    val bumped: Boolean,
    @SerialName("bumped_at")
    val bumpedAt: String,
    val archetype: String,
    val unseen: Boolean,
    val pinned: Boolean,
    val unpinned: Boolean? = null,
    val visible: Boolean,
    val closed: Boolean,
    val archived: Boolean,
    val bookmarked: Boolean? = null,
    val liked: Boolean? = null,
    val thumbnails: List<DiscourseImage>? = null,
    @SerialName("has_accepted_answer")
    val hasAcceptedAnswer: Boolean,
    @SerialName("can_have_answer")
    val canHaveAnswer: Boolean? = null,
    @SerialName("last_poster")
    val lastPoster: DiscourseLastPoster? = null,
)
