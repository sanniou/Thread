package ai.saniou.thread.domain.model

// 趋势项，列表中的每一条数据
data class TrendItem(
    val topicId: String,
    val sourceId: String,
    val title: String?,
    val contentPreview: String?,
    val rank: Int?, // Optional ranking
    val hotness: String?, // e.g., "34.5k views"
    val channel: String?, // e.g., "综合版1"
    val author: String?,
    val isNew: Boolean = false,
    val payload: Map<String, Any> = emptyMap(), // For source-specific data
    val receiveDate: Long = 0L, // For realtime sorting
)

// 趋势 Tab，用于 UI 上的分类
data class TrendTab(
    val id: String, // Unique ID for this tab within its source
    val name: String, // Display name, e.g., "热议"
    val supportsHistory: Boolean = false, // Does this tab support date picking?
    val payload: Map<String, String> = emptyMap(), // For source-specific parameters
)

// 趋势请求参数
data class TrendParams(
    val dayOffset: Int = 0, // 0 = today, 1 = yesterday...
    val refreshId: Long = 0, // 0 = default/cache, >0 = force refresh version
)
