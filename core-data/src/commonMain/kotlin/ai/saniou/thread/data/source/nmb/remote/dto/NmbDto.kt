package ai.saniou.thread.data.source.nmb.remote.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// region Base Interfaces
interface IBaseAuthor {
    val now: String
    val userHash: String
    val name: String
}

interface IThreadBody {
    val content: String
    val img: String
    val ext: String
}

interface IBaseThread : IBaseAuthor, IThreadBody {
    val id: Long
    val fid: Long
    val replyCount: Long
    val sage: Long
    val admin: Long
    val hide: Long
    val title: String
}
// endregion

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Forum(
    override val id: Long,
    override val fid: Long,
    @JsonNames("ReplyCount")
    override val replyCount: Long,
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    override val title: String,
    override val content: String,
    override val sage: Long,
    override val admin: Long,
    @JsonNames("Hide")
    override val hide: Long,
    @JsonNames("Replies")
    val replies: List<ThreadReply>,
    @JsonNames("RemainReplies")
    val remainReplies: Long? = null,
) : IBaseThread, IBaseThreadReply

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Thread(
    override val id: Long,
    override val fid: Long,
    @JsonNames("ReplyCount")
    override val replyCount: Long,
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    override val title: String,
    override val content: String,
    override val sage: Long,
    override val admin: Long,
    @JsonNames("Hide")
    override val hide: Long,
    @JsonNames("Replies")
    val replies: List<ThreadReply>,
) : IBaseThread, IThreadBody

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
    val threadId: Long = 0,
) : IBaseAuthor, IThreadBody

@Serializable
data class ForumCategory(
    val id: Long,
    val name: String,
    val status: String,
    val sort: String,
    val forums: List<ForumDetail>
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ForumDetail(
    val id: Long,
    @JsonNames("fgroup")
    val fgroup: Long,
    val sort: Long,
    val name: String,
    val showName: String,
    val msg: String,
    val interval: String,
    val createdAt: String,
    val updateAt: String,
    val status: String
)

interface IBaseThreadReply : IBaseAuthor, IThreadBody