package ai.saniou.thread.domain.model

enum class FeedType {
    RSS, JSON, HTML
}

data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val type: FeedType,
    val description: String? = null,
    val iconUrl: String? = null,
    val lastUpdate: Long = 0,
    // 对于 HTML 解析，可能需要额外的配置，如 CSS 选择器
    val selectorConfig: Map<String, String> = emptyMap(),
    val isRefreshing: Boolean = false,
    val autoRefresh: Boolean = true,
    val refreshInterval: Long = 3600000 // 1 hour in milliseconds
)