package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

/**
 * 话题/主题的元数据，不包含内容。
 * 用于在详情页展示标题、作者等信息，而将内容完全交给 Comment 流处理。
 *
 * @param id 话题唯一ID
 * @param channelId 所属频道ID
 * @param channelName 所属频道名称
 * @param title 标题
 * @param author 作者信息
 * @param createdAt 创建时间
 * @param commentCount 评论/回复数量
 * @param isSage 是否SAGE
 * @param isAdmin 是否管理员
 * @param isHidden 是否隐藏
 * @param sourceName 来源标识
 * @param sourceUrl 来源URL
 * @param lastViewedCommentId 最后阅读的评论ID
 */
data class TopicMetadata(
    val id: String,
    val channelId: String,
    val channelName: String,
    val title: String?,
    val author: Author,
    val createdAt: Instant,
    val commentCount: Long,
    val isSage: Boolean,
    val isAdmin: Boolean,
    val isHidden: Boolean,
    val sourceName: String,
    val sourceUrl: String,
    val lastViewedCommentId: String? = null
)