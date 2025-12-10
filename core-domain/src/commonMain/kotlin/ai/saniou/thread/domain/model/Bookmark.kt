package ai.saniou.thread.domain.model

import kotlin.time.Instant

/**
 * 收藏的领域模型
 */
sealed class Bookmark(
    open val id: String,
    open val createdAt: Instant,
    open val tags: List<Tag>,
) {
    /**
     * 纯文本收藏
     */
    data class Text(
        override val id: String,
        override val createdAt: Instant,
        override val tags: List<Tag>,
        val content: String,
    ) : Bookmark(id, createdAt, tags)

    /**
     * 引用另一篇帖子的收藏
     */
    data class Quote(
        override val id: String,
        override val createdAt: Instant,
        override val tags: List<Tag>,
        val content: String,
        val sourceId: String,
        val sourceType: String, // e.g., "post", "article"
    ) : Bookmark(id, createdAt, tags)

    /**
     * URL 收藏
     */
    data class Link(
        override val id: String,
        override val createdAt: Instant,
        override val tags: List<Tag>,
        val url: String,
        val title: String?,
        val description: String?,
        val favicon: String?,
    ) : Bookmark(id, createdAt, tags)

    /**
     * 图片收藏
     */
    data class Image(
        override val id: String,
        override val createdAt: Instant,
        override val tags: List<Tag>,
        val url: String,
        val width: Int?,
        val height: Int?,
    ) : Bookmark(id, createdAt, tags)

    /**
     * 媒体文件收藏 (视频/音频)
     */
    data class Media(
        override val id: String,
        override val createdAt: Instant,
        override val tags: List<Tag>,
        val url: String,
        val mimeType: String,
        val duration: Long?, // in seconds
    ) : Bookmark(id, createdAt, tags)
}
