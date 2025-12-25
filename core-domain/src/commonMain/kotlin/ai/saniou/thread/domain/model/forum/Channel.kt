package ai.saniou.thread.domain.model.forum

/**
 * 频道/板块模型 (原 Forum)
 *
 * @param id 唯一ID
 * @param name 名称
 * @param displayName 显示名称
 * @param description 描述信息
 * @param sourceName 来源名称
 */
data class Channel(
    val id: String,
    val name: String,
    val displayName: String?,
    val description: String,
    val groupId: String,
    val groupName: String,
    val sourceName: String,
    val tag: String? = null,
    val topicCount: Long?, // 原 threadCount
    val autoDelete: Long?,
    val interval: Long? = null, //发串的间隔时间，单位为秒
    val safeMode: String? = null,
)