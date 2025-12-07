package ai.saniou.thread.data.source.nmb.remote.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LastPost(
    val id: Long,//串的 ID
    val resto: Long,//回复的串的 ID，如果是发新的串则为 0
    val now: String,
    @JsonNames("user_hash")
    val userHash: String,
    val name: String,
    val email: String,
    val title: String,
    val content: String,
    val sage: Long,
    val admin: Long
)

