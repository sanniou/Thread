package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

/**
 * 话题/主题模型 (原 Post)
 *
 * @param id 话题唯一ID
 * @param channelId 所属频道ID
 * @param channelName 所属频道名称
 * @param title 标题
 * @param content 内容
 * @param author 作者信息
 * @param createdAt 创建时间
 * @param commentCount 评论/回复数量 (原 replyCount)
 * @param images 图片列表
 * @param isSage 是否SAGE
 * @param isAdmin 是否管理员
 * @param isHidden 是否隐藏
 * @param isLocal 是否本地记录
 * @param sourceName 来源标识
 * @param sourceUrl 来源URL
 * @param comments 评论列表 (原 replies)
 * @param remainingCount 剩余评论数
 * @param lastViewedCommentId 最后阅读的评论ID
 */
data class Topic(
    val id: String,
    val channelId: String,
    val channelName: String,

    val title: String?,
    val content: String,
    val summary: String?,

    val author: Author,

    val createdAt: Instant,

    val commentCount: Long,
    val images: List<Image> = emptyList(),

    // 状态标记
    val isSage: Boolean,
    val isAdmin: Boolean,
    val isHidden: Boolean,
    val isLocal: Boolean = false,

    // 来源信息
    val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,

    // 详情页数据
    val comments: List<Comment> = emptyList(),
    val remainingCount: Long? = null,
    val lastViewedCommentId: String? = null,
    val orderKey: Long? = null,
)
