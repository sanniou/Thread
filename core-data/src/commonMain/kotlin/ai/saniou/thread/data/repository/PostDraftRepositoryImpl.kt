package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.forum.PostAttachment
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import ai.saniou.thread.domain.repository.PostDraftRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PostDraftRepositoryImpl(
    private val settings: SettingsRepository,
) : PostDraftRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun get(key: PostDraftKey): SavedPostDraft? {
        val raw = settings.getValue<String>(storageKey(key)) ?: return null
        return runCatching { json.decodeFromString<PersistedPostDraft>(raw).toDomain(key) }.getOrNull()
    }

    override suspend fun save(draft: SavedPostDraft) {
        if (draft.draft.isEmpty()) {
            discard(draft.key)
            return
        }
        require((draft.draft.attachment?.bytes?.size ?: 0) <= MAX_ATTACHMENT_BYTES) {
            "草稿附件不能超过 10 MiB"
        }
        settings.saveValue(storageKey(draft.key), json.encodeToString(PersistedPostDraft.fromDomain(draft)))
    }

    override suspend fun discard(key: PostDraftKey) {
        settings.saveValue<String>(storageKey(key), null)
    }

    private fun storageKey(key: PostDraftKey) =
        "post_draft_v1:${key.sourceId}:${key.targetKind.name.lowercase()}:${key.targetId}"

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024
    }
}

private fun PostDraft.isEmpty() = content.isBlank() && name.isNullOrBlank() && title.isNullOrBlank() && attachment == null

@Serializable
private data class PersistedPostDraft(
    val version: Int = 1,
    val sourceId: String,
    val targetKind: String,
    val targetId: String,
    val content: String = "",
    val name: String? = null,
    val title: String? = null,
    val water: Boolean = false,
    val attachment: PersistedPostAttachment? = null,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain(expectedKey: PostDraftKey): SavedPostDraft {
        val persistedKey = PostDraftKey(
            sourceId,
            PostDraftTargetKind.valueOf(targetKind),
            targetId,
        )
        require(persistedKey == expectedKey) { "Draft storage key mismatch" }
        return SavedPostDraft(
            version = SavedPostDraft.CURRENT_VERSION,
            key = persistedKey,
            draft = PostDraft(content, name, title, water, attachment?.toDomain()),
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    companion object {
        fun fromDomain(value: SavedPostDraft) = PersistedPostDraft(
            version = value.version,
            sourceId = value.key.sourceId,
            targetKind = value.key.targetKind.name,
            targetId = value.key.targetId,
            content = value.draft.content.take(200_000),
            name = value.draft.name?.take(200),
            title = value.draft.title?.take(500),
            water = value.draft.water,
            attachment = value.draft.attachment?.let(PersistedPostAttachment::fromDomain),
            updatedAtEpochMillis = value.updatedAtEpochMillis,
        )
    }
}

@Serializable
private data class PersistedPostAttachment(
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String,
) {
    fun toDomain() = PostAttachment(fileName, bytes, contentType)

    companion object {
        fun fromDomain(value: PostAttachment) = PersistedPostAttachment(
            value.fileName.take(255),
            value.bytes,
            value.contentType.take(120),
        )
    }
}
