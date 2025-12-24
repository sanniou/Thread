package ai.saniou.thread.data.source.acfun.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcfunCommentListResponse(
    @SerialName("result") val result: Int,
    @SerialName("rootComments") val rootComments: List<AcfunComment>? = null,
    @SerialName("subCommentsMap") val subCommentsMap: Map<String, AcfunCommentListWrapper>? = null,
    @SerialName("pcursor") val pcursor: String? = null,
    @SerialName("error_msg") val message: String? = null
)

@Serializable
data class AcfunCommentListWrapper(
    @SerialName("list") val list: List<AcfunComment>? = null
)

@Serializable
data class AcfunComment(
    @SerialName("commentId") val commentId: Long,
    @SerialName("content") val content: String? = null,
    @SerialName("user") val user: AcfunUser? = null, // Assuming AcfunUser is reused
    @SerialName("postDate") val postDate: String? = null, // yyyy-MM-dd HH:mm:ss
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("floor") val floor: Int? = null,
    @SerialName("deviceModel") val deviceModel: String? = null,
    @SerialName("isLiked") val isLiked: Boolean? = false,
    @SerialName("likeCount") val likeCount: Int? = 0,
    @SerialName("subCommentCount") val subCommentCount: Int? = 0,
    @SerialName("subComments") val subComments: List<AcfunComment>? = null, // For nested comments if any
    @SerialName("replyTo") val replyTo: Long? = null,
    @SerialName("replyToUserName") val replyToUserName: String? = null
)