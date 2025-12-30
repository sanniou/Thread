package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseTopic(
    val id: Long,
    val title: String,
    @SerialName("fancy_title")
    val fancyTitle: String,
    val slug: String,
    @SerialName("posts_count")
    val postsCount: Int,
    @SerialName("reply_count")
    val replyCount: Int,
    @SerialName("highest_post_number")
    val highestPostNumber: Int,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val excerpt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_posted_at")
    val lastPostedAt: String,
    val bumped: Boolean,
    @SerialName("bumped_at")
    val bumpedAt: String,
    @SerialName("archetype")
    val archetype: String,
    val unseen: Boolean,
    val pinned: Boolean,
    val unpinned: Boolean? = null,
    val visible: Boolean,
    val closed: Boolean,
    val archived: Boolean,
    val bookmarked: Boolean? = null,
    val liked: Boolean? = null,
    val tags: List<String>? = null,
    @SerialName("category_id")
    val categoryId: Long,
    @SerialName("has_summary")
    val hasSummary: Boolean? = null,
    @SerialName("last_poster_username")
    val lastPosterUsername: String? = null,
    val posters: List<DiscourseTopicPoster>
)
