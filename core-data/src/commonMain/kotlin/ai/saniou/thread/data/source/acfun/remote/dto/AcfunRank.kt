package ai.saniou.thread.data.source.acfun.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcfunRankResponse(
    @SerialName("result") val result: Int,
    @SerialName("host-name") val hostName: String? = null,
    @SerialName("rankList") val rankList: List<AcfunRankItem> = emptyList(),
    @SerialName("requestId") val requestId: String? = null
)

@Serializable
data class AcfunRankItem(
    @SerialName("groupId") val groupId: String? = null,
    @SerialName("fansCount") val fansCount: Int? = null,
    @SerialName("resourceId") val resourceId: Long,
    @SerialName("resourceType") val resourceType: String? = null,
    @SerialName("channelName") val channelName: String? = null,
    @SerialName("contentId") val contentId: Long? = null,
    @SerialName("channelId") val channelId: Int? = null,
    @SerialName("bananaCount") val bananaCount: Int? = null,
    @SerialName("isFollowing") val isFollowing: Boolean? = null,
    @SerialName("viewCount") val viewCount: Long? = null,
    @SerialName("commentCount") val commentCount: Int? = null,
    @SerialName("sourcePlatform") val sourcePlatform: String? = null,
    @SerialName("tagList") val tagList: List<AcfunTag>? = null,
    @SerialName("authorId") val authorId: Long? = null,
    @SerialName("userImg") val userImg: String? = null,
    @SerialName("userName") val userName: String? = null,
    @SerialName("shareUrl") val shareUrl: String? = null,
    @SerialName("viewCountShow") val viewCountShow: String? = null,
    @SerialName("danmakuCountShow") val danmakuCountShow: String? = null,
    @SerialName("bananaCountShow") val bananaCountShow: String? = null,
    @SerialName("commentCountTenThousandShow") val commentCountTenThousandShow: String? = null,
    @SerialName("videoCover") val videoCover: String? = null,
    @SerialName("userSignature") val userSignature: String? = null,
    @SerialName("contributionCount") val contributionCount: Int? = null,
    @SerialName("danmuCount") val danmuCount: Int? = null,
    @SerialName("contributeTime") val contributeTime: Long? = null,
    @SerialName("contentTitle") val contentTitle: String? = null,
    @SerialName("contentType") val contentType: Int? = null,
    @SerialName("status") val status: Int? = null,
    @SerialName("channel") val channel: AcfunChannel? = null,
    @SerialName("userId") val userId: Long? = null
)

@Serializable
data class AcfunTag(
    @SerialName("name") val name: String,
    @SerialName("id") val id: Int
)

@Serializable
data class AcfunChannel(
    @SerialName("parentId") val parentId: Int,
    @SerialName("parentName") val parentName: String,
    @SerialName("name") val name: String,
    @SerialName("id") val id: Int
)