package ai.saniou.thread.domain.model.forum

/**
 * 话题标签
 *
 * @param id 标签ID
 * @param name 标签名称
 * @param color 标签颜色 (Hex String, e.g. "#FF0000")
 * @param url 点击跳转链接 (可选)
 */
data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
    val url: String? = null
)