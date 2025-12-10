@file:OptIn(ExperimentalTime::class)

package ai.saniou.thread.domain.model

import kotlin.time.ExperimentalTime

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
    val interval: Long? = null, //发串的间隔时间，单位为秒
    val safeMode: String? = null, //?
)
