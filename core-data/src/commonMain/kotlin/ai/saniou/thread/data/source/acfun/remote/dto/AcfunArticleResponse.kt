package ai.saniou.thread.data.source.acfun.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcfunArticleResponse(
    @SerialName("result") val result: Int,
    @SerialName("articleId") val articleId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("viewCount") val viewCount: Int? = null,
    @SerialName("commentCount") val commentCount: Int? = null,
    @SerialName("stowCount") val stowCount: Int? = null, // 收藏数
    @SerialName("shareCount") val shareCount: Int? = null,
    @SerialName("bananaCount") val bananaCount: Int? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("createTimeMillis") val createTimeMillis: Long? = null,
    @SerialName("createTime") val createTime: String? = null,
    @SerialName("user") val user: AcfunUser? = null,
    @SerialName("channel") val channel: AcfunChannel? = null,
    @SerialName("parts") val parts: List<AcfunArticlePart>? = null,
    @SerialName("realm") val realm: AcfunRealm? = null,
)

@Serializable
data class AcfunUser(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("headUrl") val headUrl: String? = null,
    @SerialName("signature") val signature: String? = null,
)

@Serializable
data class AcfunArticlePart(
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String, // HTML content
)

@Serializable
data class AcfunRealm(
    @SerialName("realmId") val realmId: Int,
    @SerialName("realmName") val realmName: String,
)
