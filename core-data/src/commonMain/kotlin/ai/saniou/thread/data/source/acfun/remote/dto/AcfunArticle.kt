//package ai.saniou.thread.data.source.acfun.remote.dto
//
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class AcfunArticleResponse(
//    @SerialName("result") val result: Int,
//    @SerialName("data") val data: AcfunArticleData? = null,
//    @SerialName("error_msg") val message: String? = null
//)
//
//@Serializable
//data class AcfunArticleData(
//    @SerialName("articleId") val articleId: Long,
//    @SerialName("contentTitle") val contentTitle: String,
//    @SerialName("coverUrl") val coverUrl: String? = null,
//    @SerialName("viewCount") val viewCount: Long? = null,
//    @SerialName("commentCount") val commentCount: Long? = null,
//    @SerialName("bananaCount") val bananaCount: Long? = null,
//    @SerialName("stowCount") val stowCount: Long? = null,
//    @SerialName("shareCount") val shareCount: Long? = null,
//    @SerialName("user") val user: AcfunUser? = null,
//    @SerialName("createTimeMillis") val createTimeMillis: Long? = null,
//    @SerialName("parts") val parts: List<AcfunArticlePart>? = null,
//    @SerialName("description") val description: String? = null,
//    @SerialName("tagList") val tagList: List<AcfunTag>? = null,
//    @SerialName("channel") val channel: AcfunChannel? = null
//)
//
//@Serializable
//data class AcfunArticlePart(
//    @SerialName("content") val content: String, // HTML content
//    @SerialName("title") val title: String? = null
//)
//
//@Serializable
//data class AcfunUser(
//    @SerialName("userId") val userId: Long,
//    @SerialName("userName") val userName: String,
//    @SerialName("headUrl") val headUrl: String? = null,
//    @SerialName("signature") val signature: String? = null
//)
