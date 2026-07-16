package ai.saniou.thread.domain.model.forum

enum class PostDraftTargetKind { CHANNEL, TOPIC }

data class PostDraftKey(
    val sourceId: String,
    val targetKind: PostDraftTargetKind,
    val targetId: String,
) {
    init {
        require(sourceId.isNotBlank())
        require(targetId.isNotBlank())
    }
}

data class SavedPostDraft(
    val version: Int = CURRENT_VERSION,
    val key: PostDraftKey,
    val draft: PostDraft,
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
