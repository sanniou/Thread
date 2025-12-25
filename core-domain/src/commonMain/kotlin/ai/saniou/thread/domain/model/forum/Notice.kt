package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

data class Notice(
    val id: String,
    val content: String,
    val date: Instant,
    val enable: Boolean,
    val isRead: Boolean
)