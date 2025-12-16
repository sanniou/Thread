package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseLatestPostsResponse(
    val users: List<DiscourseUser>,
    @SerialName("topic_list")
    val topicList: DiscourseTopicList
)

@Serializable
data class DiscourseUser(
    val id: Long,
    val username: String,
    val name: String?,
    @SerialName("avatar_template")
    val avatarTemplate: String
)

@Serializable
data class DiscourseTopicList(
    @SerialName("can_create_topic")
    val canCreateTopic: Boolean,
    val more_topics_url: String? = null,
    val topics: List<DiscourseTopic>
)

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

@Serializable
data class DiscourseTopicPoster(
    val extras: String? = null,
    val description: String,
    @SerialName("user_id")
    val userId: Long,
    @SerialName("primary_group_id")
    val primaryGroupId: Long? = null
)

@Serializable
data class DiscourseCategoriesResponse(
    @SerialName("category_list")
    val categoryList: DiscourseCategoryList
)

@Serializable
data class DiscourseCategoryList(
    @SerialName("can_create_category")
    val canCreateCategory: Boolean,
    @SerialName("can_create_topic")
    val canCreateTopic: Boolean,
    val categories: List<DiscourseCategory>
)

@Serializable
data class DiscourseCategory(
    val id: Long,
    val name: String,
    val color: String,
    @SerialName("text_color")
    val textColor: String,
    val slug: String,
    @SerialName("topic_count")
    val topicCount: Int,
    @SerialName("post_count")
    val postCount: Int,
    val description: String?,
    @SerialName("description_text")
    val descriptionText: String?,
    @SerialName("topic_url")
    val topicUrl: String? = null,
    @SerialName("read_restricted")
    val readRestricted: Boolean,
    val permission: Int? = null,
    @SerialName("parent_category_id")
    val parentCategoryId: Long? = null,
    @SerialName("notification_level")
    val notificationLevel: Int? = null,
    @SerialName("uploaded_logo")
    val uploadedLogo: DiscourseImage? = null,
    @SerialName("uploaded_background")
    val uploadedBackground: DiscourseImage? = null
)

@Serializable
data class DiscourseImage(
    val id: Long,
    val url: String,
    val width: Int,
    val height: Int
)