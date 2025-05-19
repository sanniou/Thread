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
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    val title: String,
    override val content: String,
    val sage: Long,
    val admin: Long,
    @JsonNames("Hide")
    val hide: Long,
    @JsonNames("Replies")
    val replies: List<ThreadReply>,
) : IBaseAuthor, IThreadBody

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ThreadReply(
    val id: Long,
    @JsonNames("user_hash")
    override val userHash: String,
    val admin: Long,
    val title: String,
    override val now: String,
    override val content: String,
    override val img: String,
    override val ext: String,
    override val name: String,
) : IBaseAuthor, IThreadBody
