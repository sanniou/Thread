package ai.saniou.thread.domain.model

import kotlinx.datetime.Instant

data class Article(
    val id: String,
    val feedSourceId: String,
    val title: String,
    val content: String, // 或者是摘要
    val link: String,
    val author: String? = null,
    val publishDate: Instant,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    // 原始数据，以便未来重新解析或展示
    val rawData: String? = null
)