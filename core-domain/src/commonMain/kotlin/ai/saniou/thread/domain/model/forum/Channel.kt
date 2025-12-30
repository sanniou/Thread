package ai.saniou.thread.domain.model.forum

/**
 * 频道/板块模型
 *
 * @param id 唯一ID
 * @param name 名称
 * @param displayName 显示名称
 * @param description 描述信息 (HTML)
 * @param descriptionText 纯文本描述 (用于列表摘要)
 * @param sourceName 来源名称
 * @param sort 排序权重
 * @param topicCount 主题数
 * @param postCount 帖子/回复总数 (反映热度)
 * @param color 板块主色调 (Hex)
 * @param textColor 文本颜色 (Hex)
 * @param icon FontAwesome 图标名
 * @param emoji Emoji 表情 (部分板块替代图标)
 * @param styleType 板块显示风格 (如 "icon")
 * @param listViewStyle 子板块列表显示风格 (如 "boxes")
 * @param logoUrl Logo 图片链接
 * @param bannerUrl Banner 图片链接
 * @param slug URL 路径标识
 * @param canCreateTopic 是否允许发帖
 */
data class Channel(
    val id: String,
    val name: String,
    val displayName: String?,
    val description: String,
    val descriptionText: String?,
    val groupId: String,
    val groupName: String,
    val sourceName: String,
    val sort: Long?,
    val tag: String? = null,
    val topicCount: Long?,
    val postCount: Long?,
    val autoDelete: Long?,
    val interval: Long? = null, //发串的间隔时间，单位为秒
    val safeMode: String? = null,
    val parentId: String? = null,
    val color: String? = null,
    val textColor: String? = null,
    val icon: String? = null,
    val emoji: String? = null,
    val styleType: String? = null,
    val listViewStyle: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val slug: String? = null,
    val canCreateTopic: Boolean? = null,
    val children: List<Channel> = emptyList(),
)
