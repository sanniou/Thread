package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class PostReplyRequest(
    val name: String? = null,
    val title: String? = null,
    val content: String? = null,
    @JsonNames("resto")
    val resTo: Int? = null,
    val image: String? = null,
    val water: Boolean? = null,
)
