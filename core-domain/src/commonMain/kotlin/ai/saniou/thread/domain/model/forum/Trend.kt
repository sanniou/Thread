package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

data class Trend(
    val rank: String,
    val trendNum: String,
    val channel: String, // 原 forum
    val isNew: Boolean,
    val topicId: String, // 原 threadId
    val contentPreview: String
)

data class TrendResult(
    val date: Instant,
    val items: List<Trend>,
    val correctedDayOffset: Int? = null
)