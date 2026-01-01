package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant

data class Account(
    val id: String,
    val sourceId: String,
    val alias: String?,
    val value: String,
    val uid: String?,
    val avatar: String?,
    val extraData: String?,
    val sort: Long,
    val isCurrent: Boolean,
    val lastUsedAt: Instant,
    val createdAt: Instant,
)
