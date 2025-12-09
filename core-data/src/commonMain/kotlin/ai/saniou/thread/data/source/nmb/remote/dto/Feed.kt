package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.nmb.db.table.SelectSubscriptionThread
import ai.saniou.nmb.db.table.Thread
import ai.saniou.thread.domain.model.Post
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.time.ExperimentalTime

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
 *
 *     {
 *         "id": "52592201",
 *         "fid": "106",
 *         "img": "2022-10-10\/6343f92fce7ad",
 *         "ext": ".jpg",
 *         "now": "2022-10-10(一)18:51:25",
 *         "user_hash": "u4KtXpW",
 *         "name": "",
 *         "email": "",
 *         "title": "",
 *         "content": "要不然拼多多拼单开个集中串吧<br \/>\n<br \/>\n我先来",
 *         "admin": "0"
 *     }
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Feed(
    override val id: Long,// 串的 ID
    @JsonNames("user_id")
    val userId: String? = null,// 发串的用户 ID？
    override val fid: Long,// 串所属的版面 ID
    @JsonNames("reply_count")
    override val replyCount: Long = 0,// 回复数量
    @JsonNames("recent_replies")
    val recentReplies: String? = null,// 最近几条回复的 ID，使用的是 [0,1,2,3] 这样的类似于数组的格式
    val category: String? = null,// ？
    @JsonNames("file_id")
    val fileId: String? = null,// ？
    override val img: String,
    override val ext: String,
    override val now: String,
    @JsonNames("user_hash")
    override val userHash: String,
    override val name: String,
    val email: String,
    override val title: String,
    override val content: String,
    val status: String? = null,// ？
    override val admin: Long,
    override val hide: Long = 0,
    val po: String? = null,// ？
    override val sage: Long = 0,
    val isLocal: Boolean = false,
) : IBaseThread


fun Thread.toFeed(isLocal: Boolean = false) = Feed(
    id = id,
    fid = fid,
    replyCount = replyCount,
    img = img,
    ext = ext,
    title = title,
    content = content,
    admin = admin,
    hide = hide,
    now = now,
    userHash = userHash,
    name = name,
    sage = sage,
    userId = "",
    recentReplies = "[]",
    category = "",
    fileId = "",
    email = "",
    status = "",
    po = "",
    isLocal = isLocal
)

fun SelectSubscriptionThread.toFeed() = Feed(
    id = id,
    fid = fid,
    replyCount = replyCount,
    img = img,
    ext = ext,
    title = title,
    content = content,
    admin = admin,
    hide = hide,
    now = now,
    userHash = userHash,
    name = name,
    sage = sage,
    userId = "",
    recentReplies = "[]",
    category = "",
    fileId = "",
    email = "",
    status = "",
    po = "",
    isLocal = isLocal == 1L
)

fun Feed.toTable(page: Long) = Thread(
    id = id,
    fid = fid,
    replyCount = replyCount,
    img = img,
    ext = ext,
    title = title,
    content = content,
    admin = admin,
    hide = hide,
    now = now,
    userHash = userHash,
    name = name,
    sage = sage,
    page = page
)

@OptIn(ExperimentalTime::class)

fun Feed.nowToEpochMilliseconds(): Long {
    // 原始格式：2025-11-17(一)04:10:48
    // 1. 去掉括号和星期
    val cleaned = this.now.replace(Regex("\\(.*?\\)"), "")

    // cleaned: "2025-11-1704:10:48"
    // 2. 插入一个 T，变成 ISO-8601 兼容格式
    val isoString = cleaned.take(10) + "T" + cleaned.substring(10)

    // isoString: "2025-11-17T04:10:48"

    // 3. 解析成 LocalDateTime
    val ldt = LocalDateTime.parse(isoString)

    // 4. 转为 epoch milliseconds（系统所在时区）
    return ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun IBaseThread.toDomain(): Post = Post(
    id = id.toString(),
    sourceName = "nmb",
    sourceUrl = "https://nmb.ai/thread/$id",
    title = title,
    content = content,
    author = name,
    userHash = userHash,
    createdAt = now.toTime(),
    forumName = fid.toString(),
    replyCount = replyCount,
    img = img,
    ext = ext,
    isSage = sage > 0,
    isAdmin = admin > 0,
    isHidden = hide > 0,
    isLocal = false,
    now = now,
    name = name,
    sage = sage,
    fid = fid,
    admin = admin,
    hide = hide,
    replies = null,
    remainReplies = null
)

interface IBaseThread : IBaseAuthor, IThreadBody {

    val id: Long
    val sage: Long
    val fid: Long
    val replyCount: Long
    val title: String
    val admin: Long
    val hide: Long
}

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

interface IBaseThreadReply {
    val replies: List<ThreadReply>
    val remainReplies: Long?
}
