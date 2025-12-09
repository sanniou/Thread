package ai.saniou.thread.domain.model

data class Notice(
    val id: String,
    val content: String,
    val date: Long,
    val enable: Boolean,
    val isRead: Boolean
)