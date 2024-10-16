package ai.saniou.nmb.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class TimeLine(
    val id: Int,
    val name: String,
    val display_name: String,
    val notice: String,
    val max_page: Int,
)
