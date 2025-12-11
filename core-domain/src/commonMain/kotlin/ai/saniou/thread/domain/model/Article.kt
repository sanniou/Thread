package ai.saniou.thread.domain.model

import kotlinx.datetime.Instant

data class Article(
    val id: String,
    val feedSourceId: String,
    val title: String,
    val description: String, // 摘要，纯文本，用于列表页
    val content: String,     // 清理后的正文，用于原生渲染。可以等于 description。
    val link: String,
    val author: String? = null,
    val publishDate: Instant,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val imageUrl: String? = null,
    // 原始 HTML 内容，用于 WebView 渲染或重新解析
    val rawContent: String? = null
)