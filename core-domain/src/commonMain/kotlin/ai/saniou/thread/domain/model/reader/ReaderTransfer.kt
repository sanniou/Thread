package ai.saniou.thread.domain.model.reader

enum class ReaderSubscriptionFormat {
    JSON,
    OPML,
}

data class ReaderImportReport(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val feedSourceIds: Set<String> = emptySet(),
)

data class ReaderSchedulerState(
    val isRunning: Boolean = false,
    val dueCount: Int = 0,
    val refreshingSourceIds: Set<String> = emptySet(),
    val lastRunEpochMillis: Long? = null,
)
