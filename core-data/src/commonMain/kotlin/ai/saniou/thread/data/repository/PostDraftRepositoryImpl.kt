package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.forum.PostAttachment
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import ai.saniou.thread.domain.repository.PostDraftRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class PostDraftRepositoryImpl(
    private val settings: SettingsRepository,
) : PostDraftRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    override fun observeAll(): Flow<List<SavedPostDraft>> =
        settings.observeValue<String>(INDEX_KEY).mapLatest { raw ->
            decodeIndex(raw).mapNotNull { entry -> get(entry.toDomain()) }
                .sortedByDescending(SavedPostDraft::updatedAtEpochMillis)
        }

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
        mutex.withLock {
            settings.saveValue(storageKey(draft.key), json.encodeToString(PersistedPostDraft.fromDomain(draft)))
            val index = readIndex().associateByTo(linkedMapOf(), PersistedDraftIndexEntry::stableKey)
            index[draft.key.stableKey] = PersistedDraftIndexEntry.fromDomain(draft)
            val retained = index.values.sortedByDescending(PersistedDraftIndexEntry::updatedAtEpochMillis)
                .take(MAX_DRAFT_COUNT)
            writeIndex(retained)
            val retainedKeys = retained.mapTo(mutableSetOf(), PersistedDraftIndexEntry::stableKey)
            index.values.filter { it.stableKey !in retainedKeys }.forEach { evicted ->
                settings.saveValue<String>(storageKey(evicted.toDomain()), null)
            }
        }
    }

    override suspend fun discard(key: PostDraftKey) {
        mutex.withLock {
            val index = readIndex().filterNot { it.stableKey == key.stableKey }
            writeIndex(index)
            settings.saveValue<String>(storageKey(key), null)
        }
    }

    private suspend fun readIndex(): List<PersistedDraftIndexEntry> =
        decodeIndex(settings.getValue<String>(INDEX_KEY))

    private fun decodeIndex(raw: String?): List<PersistedDraftIndexEntry> = raw?.let {
        runCatching { json.decodeFromString<List<PersistedDraftIndexEntry>>(it) }.getOrDefault(emptyList())
    }.orEmpty()

    private suspend fun writeIndex(entries: List<PersistedDraftIndexEntry>) {
        settings.saveValue(
            INDEX_KEY,
            entries.sortedByDescending(PersistedDraftIndexEntry::updatedAtEpochMillis)
                .take(MAX_DRAFT_COUNT)
                .let(json::encodeToString),
        )
    }

    private fun storageKey(key: PostDraftKey) =
        "post_draft_v1:${key.sourceId}:${key.targetKind.name.lowercase()}:${key.targetId}"

    private companion object {
        const val INDEX_KEY = "post_draft_index_v1"
        const val MAX_DRAFT_COUNT = 100
        const val MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024
    }
}

@Serializable
private data class PersistedDraftIndexEntry(
    val sourceId: String,
    val targetKind: String,
    val targetId: String,
    val updatedAtEpochMillis: Long,
) {
    val stableKey: String get() = "$sourceId:${targetKind.lowercase()}:$targetId"

    fun toDomain() = PostDraftKey(sourceId, PostDraftTargetKind.valueOf(targetKind), targetId)

    companion object {
        fun fromDomain(value: SavedPostDraft) = PersistedDraftIndexEntry(
            sourceId = value.key.sourceId,
            targetKind = value.key.targetKind.name,
            targetId = value.key.targetId,
            updatedAtEpochMillis = value.updatedAtEpochMillis,
        )
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
