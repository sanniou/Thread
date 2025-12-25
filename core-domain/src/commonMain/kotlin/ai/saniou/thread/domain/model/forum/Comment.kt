package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

/**
 * 评论/回复模型 (原 ThreadReply)
 *
 * @param id 评论ID
 * @param topicId 所属话题ID (原 threadId)
 * @param author 作者信息
 * @param createdAt 创建时间
 * @param title 标题 (通常为空)
 * @param content 内容
 * @param images 图片列表
 * @param isAdmin 是否管理员
 * @param floor 楼层号 (可选，部分源可能没有)
 * @param replyToId 回复目标ID (可选，用于引用回复)
 */
data class Comment(
    val id: String,
    val topicId: String,
    val author: Author,
    val createdAt: Instant,
    val title: String?,
    val content: String,
    val images: List<Image> = emptyList(),
    val isAdmin: Boolean,
    val floor: Int? = null,
    val replyToId: String? = null
)