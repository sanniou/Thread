package ai.saniou.thread.domain.model

/**
 * 收藏的领域模型
 *
 * @param postId 帖子ID
 * @param content 帖子内容摘要
 * @param tag 标签
 * @param createdAt 创建时间戳
 */
data class Bookmark(
    val postId: String,
    val content: String,
    val tag: String?,
    val createdAt: Long
)