package ai.saniou.thread.domain.model

data class Cookie(
    val alias: String?,
    val value: String,
    val sort: Long,
    val lastUsedAt: Long,
    val createdAt: Long,
)
