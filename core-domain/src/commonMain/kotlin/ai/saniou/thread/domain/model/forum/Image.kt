package ai.saniou.thread.domain.model.forum

/**
 * 图片的领域模型
 *
 * @param originalUrl 原图链接
 * @param thumbnailUrl 缩略图链接
 * @param name 图片名称
 * @param extension 图片扩展名
 * @param width 图片宽度
 * @param height 图片高度
 */
data class Image(
    val originalUrl: String,
    val thumbnailUrl: String,
    val name: String? = null,
    val extension: String? = null,
    val width: Int? = null,
    val height: Int? = null
)