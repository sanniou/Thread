package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

data class Cookie(
    val alias: String?,
    val value: String,
    val sort: Long,
    val lastUsedAt: Instant,
    val createdAt: Instant,
)
