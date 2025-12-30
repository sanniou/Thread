package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

data class Trend(
    val rank: String,
    val trendNum: String,
    val channel: String,
    val isNew: Boolean,
    val topicId: String,
    val contentPreview: String,
)

data class TrendResult(
    val date: Instant,
    val items: List<Trend>,
    val correctedDayOffset: Int? = null,
)
