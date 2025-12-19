package ai.saniou.thread.domain.model.forum

import kotlin.time.Instant


data class ThreadReply(
    val id: String,
    val userHash: String,
    val admin: Long,
    val title: String,
    val now: String,
    val createdAt: Instant,
    val content: String,
    val img: String,
    val ext: String,
    val name: String,
    val threadId: String,
)
