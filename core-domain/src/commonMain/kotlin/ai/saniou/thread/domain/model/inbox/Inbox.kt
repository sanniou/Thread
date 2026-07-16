package ai.saniou.thread.domain.model.inbox

import ai.saniou.thread.domain.model.content.ContentReference
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class InboxKind {
    ANNOUNCEMENT,
    MENTION,
    REPLY,
    SUBSCRIPTION_UPDATE,
    READER_UPDATE,
    SYSTEM,
}

data class InboxEvent(
    val id: String,
    val kind: InboxKind,
    val sourceId: String,
    val title: String,
    val summary: String,
    val reference: ContentReference?,
    val occurredAt: Instant,
    val readAt: Instant? = null,
    val muted: Boolean = false,
    val priority: Int = 0,
) {
    init {
        require(id.isNotBlank())
        require(sourceId.isNotBlank())
        require(title.isNotBlank())
        require(priority in 0..3)
    }

    val isRead: Boolean get() = readAt != null
}

data class InboxFilter(
    val unreadOnly: Boolean = false,
    val sourceId: String? = null,
    val kind: InboxKind? = null,
    val includeMuted: Boolean = false,
    val query: String = "",
)

data class InboxSourceCount(
    val sourceId: String,
    val total: Int,
    val unread: Int,
    val muted: Boolean,
)

data class InboxSummary(
    val total: Int = 0,
    val unread: Int = 0,
    val muted: Int = 0,
    val sourceCounts: List<InboxSourceCount> = emptyList(),
)
