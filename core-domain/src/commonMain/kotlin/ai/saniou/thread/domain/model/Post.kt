package ai.saniou.thread.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 帖子的领域模型 (Domain Model)。
 *
 * 这是一个纯粹的数据类，代表了一个帖子的核心业务概念，与任何特定数据源（如 NMB, NGA）或 UI 框架无关。
 * 它包含了在不同来源之间通用、且对 UI 展示有意义的所有属性。
 *
 * @param id 帖子的唯一ID。
 * @param sourceName 数据来源的标识，例如 "nmb", "nga"。
 * @param sourceUrl 指向原始帖子的完整 URL。
 * @param title 帖子标题。
 * @param content 帖子内容。
 * @param author 作者的显示名称。
 * @param userHash 作者的唯一哈希，用于识别用户。
 * @param createdAt 帖子的创建时间。
 * @param forumName 帖子所属版块的名称。
 * @param replyCount 回复数量。
 * @param img 帖子的图片路径（相对路径）。
 * @param ext 图片的扩展名。
 * @param isSage 是否为 SAGE 帖子。
 * @param isAdmin 是否为管理员发布的帖子。
 * @param isHidden 是否为隐藏的帖子。
 * @param isLocal (用于订阅) 是否为仅存在于本地的记录。
 */
@OptIn(ExperimentalTime::class)
data class Post(
    val id: String,
    val sourceName: String,
    val sourceUrl: String,
    val title: String,
    val content: String,
    val author: String,
    val userHash: String,
    val createdAt: Instant,
    val forumName: String,
    val replyCount: Long,
    val img: String?,
    val ext: String?,
    val isSage: Boolean,
    val isAdmin: Boolean,
    val isHidden: Boolean,
    val isLocal: Boolean = false,
    val now: String,
    val name: String,
    val sage: Long,
    val fid: Long,
    val admin: Long,
    val hide: Long,
    val replies: List<ThreadReply>? = null,
    val remainReplies: Long? = null,
)
