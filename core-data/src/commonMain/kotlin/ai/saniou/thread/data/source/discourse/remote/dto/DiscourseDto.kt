package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("style_type")
    val styleType: String? = null,
    val icon: String? = null,
    val emoji: String? = null,
    val slug: String,
    @SerialName("topic_count")
    val topicCount: Int,
    @SerialName("post_count")
    val postCount: Int,
    val position: Int? = null,
    val description: String?,
    @SerialName("description_text")
    val descriptionText: String?,
    @SerialName("description_excerpt")
    val descriptionExcerpt: String? = null,
    @SerialName("topic_url")
    val topicUrl: String? = null,
    @SerialName("read_restricted")
    val readRestricted: Boolean,
    val permission: Int? = null,
    @SerialName("parent_category_id")
    val parentCategoryId: Long? = null,
    @SerialName("notification_level")
    val notificationLevel: Int? = null,
    @SerialName("topic_template")
    val topicTemplate: String? = null,
    @SerialName("has_children")
    val hasChildren: Boolean? = null,
    @SerialName("subcategory_count")
    val subcategoryCount: Int? = null,
    @SerialName("sort_order")
    val sortOrder: String? = null,
    @SerialName("sort_ascending")
    val sortAscending: Boolean? = null,
    @SerialName("show_subcategory_list")
    val showSubcategoryList: Boolean? = null,
    @SerialName("num_featured_topics")
    val numFeaturedTopics: Int? = null,
    @SerialName("default_view")
    val defaultView: String? = null,
    @SerialName("subcategory_list_style")
    val subcategoryListStyle: String? = null,
    @SerialName("default_top_period")
    val defaultTopPeriod: String? = null,
    @SerialName("default_list_filter")
    val defaultListFilter: String? = null,
    @SerialName("minimum_required_tags")
    val minimumRequiredTags: Int? = null,
    @SerialName("navigate_to_first_post_after_read")
    val navigateToFirstPostAfterRead: Boolean? = null,
    @SerialName("custom_fields")
    val customFields: Map<String, String?>? = null,
    @SerialName("topics_day")
    val topicsDay: Int? = null,
    @SerialName("topics_week")
    val topicsWeek: Int? = null,
    @SerialName("topics_month")
    val topicsMonth: Int? = null,
    @SerialName("topics_year")
    val topicsYear: Int? = null,
    @SerialName("topics_all_time")
    val topicsAllTime: Int? = null,
    @SerialName("subcategory_ids")
    val subcategoryIds: List<Long>? = null,
    @SerialName("uploaded_logo")
    val uploadedLogo: DiscourseImage? = null,
    @SerialName("uploaded_logo_dark")
    val uploadedLogoDark: DiscourseImage? = null,
    @SerialName("uploaded_background")
    val uploadedBackground: DiscourseImage? = null,
    @SerialName("uploaded_background_dark")
    val uploadedBackgroundDark: DiscourseImage? = null,
    val topics: List<DiscourseCategoryTopic>? = null,
    @SerialName("subcategory_list")
    val subcategoryList: List<DiscourseCategory>? = null
)

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
    val lastPoster: DiscourseLastPoster? = null
)

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

@Serializable
data class DiscourseImage(
    val id: Long? = null,
    @SerialName("max_width")
    val maxWidth: Int? = null,
    @SerialName("max_height")
    val maxHeight: Int? = null,
    val width: Int,
    val height: Int,
    val url: String
)
