package ai.saniou.thread.data.source.discourse.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoursePostStream(
    val posts: List<DiscoursePost>,
    val stream: List<Long>? = null,
)
