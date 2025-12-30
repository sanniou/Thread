package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseCategoryList(
    @SerialName("can_create_category")
    val canCreateCategory: Boolean,
    @SerialName("can_create_topic")
    val canCreateTopic: Boolean,
    val categories: List<DiscourseCategory>
)
