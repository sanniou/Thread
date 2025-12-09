package ai.saniou.thread.domain.model

/**
 * 板块详情的领域模型
 *
 * @param id 板块ID
 * @param name 板块名称
 * @param showName 显示名称
 * @param msg 板块信息
 * @param updateAt 更新时间
 * @param threadsCount 主题数
 * @param postsCount 帖子数
 */
data class ForumDetail(
    val id: Long,
    val name: String,
    val showName: String,
    val msg: String,
    val updateAt: String,
    val threadsCount: String,
    val postsCount: String,
)