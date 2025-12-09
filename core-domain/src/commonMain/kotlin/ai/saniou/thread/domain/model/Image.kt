package ai.saniou.thread.domain.model

/**
 * 图片的领域模型
 *
 * @param name 图片名称/路径
 * @param ext 图片扩展名
 */
data class Image(
    val name: String,
    val ext: String
)