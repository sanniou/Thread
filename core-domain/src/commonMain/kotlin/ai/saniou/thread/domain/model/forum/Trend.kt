package ai.saniou.thread.domain.model.forum

data class Trend(
    val rank: String,
    val trendNum: String,
    val forum: String,
    val isNew: Boolean,
    val threadId: Long,
    val contentPreview: String
)