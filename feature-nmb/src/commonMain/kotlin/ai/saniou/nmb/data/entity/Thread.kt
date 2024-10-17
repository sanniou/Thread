package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Thread(
    val id: Long,
    val fid: Long,
    @JsonNames("ReplyCount")
    val replyCount: Long,
    val img: String,
    val ext: String,
    val now: String,
    @JsonNames("user_hash")
    val userHash: String,
    val name: String,
    val title: String,
    val content: String,
    val sage: Long,
    val admin: Long,
    @JsonNames("Hide")
    val hide: Long,
    @JsonNames("Replies")
    val replies: List<ThreadReply>,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ThreadReply(
    val id: Long,
    @JsonNames("user_hash")
    val userHash: String,
    val admin: Long,
    val title: String,
    val now: String,
    val content: String,
    val img: String,
    val ext: String,
    val name: String,
)
