package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscourseTopicList(
    @SerialName("can_create_topic")
    val canCreateTopic: Boolean,
    val more_topics_url: String? = null,
    val topics: List<DiscourseTopic>,
)
