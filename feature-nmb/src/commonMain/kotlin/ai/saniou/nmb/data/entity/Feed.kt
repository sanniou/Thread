package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * [].id 	String 	串的 ID
 * [].user_id 	String 	发串的用户 ID？
 * [].fid 	String 	串所属的版面 ID
 * [].reply_count 	String 	回复数量
 * [].recent_replies 	String 	最近几条回复的 ID，使用的是 [0,1,2,3] 这样的类似于数组的格式
 * [].category 	String 	？
 * [].file_id 	String 	？
 * [].img 	String 	参见“查看版面”，不再重复
 * [].ext 	String
 * [].now 	String
 * [].user_hash 	String
 * [].name 	String
 * [].email 	String
 * [].title 	String
 * [].content 	String
 * [].status 	String
 * [].admin 	String
 * [].hide 	String
 * [].po 	String 	？
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Feed(
    val id: String,// 串的 ID
    @JsonNames("user_id")
    val userId: String,// 发串的用户 ID？
    val fid: String,// 串所属的版面 ID
    @JsonNames("reply_count")
    val replyCount: String,// 回复数量
    @JsonNames("recent_replies")
    val recentReplies: String,// 最近几条回复的 ID，使用的是 [0,1,2,3] 这样的类似于数组的格式
    val category: String,// ？
    @JsonNames("file_id")
    val fileId: String,// ？
    val img: String,
    val ext: String,
    val now: String,
    @JsonNames("user_hash")
    val userHash: String,
    val name: String,
    val email: String,
    val title: String,
    val content: String,
    val status: String,
    val admin: String,
    val hide: String,
    val po: String,// ？
)
