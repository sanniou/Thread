package ai.saniou.nmb.data.entity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * 请求错误
 * {
 *     "success": false,
 *     "error": "必须登入领取饼干后才可以访问"
 * }
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ShowF(
    override val id: Long,// 串的 ID
    override val fid: Long,// 串所属的版面 ID
    @JsonNames("ReplyCount")
    override val replyCount: Long,// 回复数量
    override val img: String,// 图片的相对地址
    override val ext: String,// 图片扩展名
    override val now: String,// 发串时间，格式：2022-06-18(六)05:10:29
    @JsonNames("user_hash")
    override val userHash: String,// 发串的饼干或红名名称
    override val name: String,// 一般是“无名氏”的名称
    override val title: String,// 一般是“无标题”的标题
    override val content: String,// 串的内容，使用 HTML
    override val sage: Long,// 是否被 SAGE，可以当成 Boolean 使用（非 0 则为 true）
    override val admin: Long,// 是否为红名小会员，可以当成 Boolean 使用
    @JsonNames("Hide")
    override val hide: Long,// ？
    @JsonNames("Replies")
    override val replies: List<Reply>,
    @JsonNames("RemainReplies")
    override val remainReplies: Long? = null,// 网页版除去显示的最近几条回复后剩余的回复数量,“回应有……篇被省略。要阅读所有回应请按下回应链接。”
) : IBaseThread, IBaseThreadReply

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Reply(
    val id: Long,// 串的 ID
    val fid: Long,// 串所属的版面 ID
    @JsonNames("ReplyCount")
    val replyCount: Long,// 回复数量
    val img: String,// 图片的相对地址
    val ext: String,// 图片扩展名
    override val now: String,// 发串时间，格式：2022-06-18(六)05:10:29
    @JsonNames("user_hash")
    override val userHash: String,// 发串的饼干或红名名称
    override val name: String,// 一般是“无名氏”的名称
    val title: String,// 一般是“无标题”的标题
    val content: String,// 串的内容，使用 HTML
    val sage: Long,// 是否被 SAGE，可以当成 Boolean 使用（非 0 则为 true）
    val admin: Long,// 是否为红名小会员，可以当成 Boolean 使用
    @JsonNames("Hide")
    val hide: Long? = null,// ？
) : IBaseAuthor
