@file:OptIn(ExperimentalTime::class)

package ai.saniou.thread.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant


/**
 * 聚合的帖子模型
 *
 * @param id 唯一ID，格式为 "source:original_id"
 * @param title 标题
 * @param content 内容
 * @param author 作者
 * @param createdAt 创建时间
 * @param sourceName 来源名称, e.g., "NMB", "NGA"
 * @param sourceUrl 原始链接
 * @param forumName 板块名称
 */
data class Post(
    val id: String,
    val title: String,
    val content: String,
    val author: String,
    val createdAt: Instant,
    val sourceName: String,
    val sourceUrl: String,
    val forumName: String,
)

/**
 * 板块模型
 *
 * @param id 唯一ID
 * @param name 名称
 * @param sourceName 来源名称
 */
data class Forum(
    val id: String,
    val name: String,
    val showName: String?,
    val msg: String,
    val groupId: String,
    val groupName: String,
    val sourceName: String,
    val tag: String? = null,
    val threadCount: Long?,
    val autoDelete: Long?,
)
