package ai.saniou.thread.domain.model

/**
 * 标签类型
 */
enum class TagType {
    SYSTEM, // 源站属性 (e.g., SAGE, Admin)
    USER,   // 用户自定义 (e.g., Read Later)
    META    // 内部元数据 (e.g., Local)
}

/**
 * 统一标签模型
 *
 * @param id 标签唯一ID
 * @param name 标签显示名称
 * @param color 标签颜色 (Hex String, e.g. "#FF0000")
 * @param icon 图标资源ID (可选)
 * @param url 点击跳转链接 (可选)
 * @param type 标签类型
 */
data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val url: String? = null,
    val type: TagType = TagType.SYSTEM
)