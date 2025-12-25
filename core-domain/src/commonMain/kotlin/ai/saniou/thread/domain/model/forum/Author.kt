package ai.saniou.thread.domain.model.forum

/**
 * 作者/用户模型
 *
 * @param id 用户唯一标识 (如 userHash)
 * @param name 显示名称
 * @param avatar 头像链接
 */
data class Author(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val sourceName: String? = null
)