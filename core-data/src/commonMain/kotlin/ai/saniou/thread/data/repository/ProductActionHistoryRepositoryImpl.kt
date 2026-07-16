package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.activity.ProductActionRecord
import ai.saniou.thread.domain.model.activity.ProductActionStatus
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.repository.ProductActionHistoryRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ProductActionHistoryRepositoryImpl(
    private val settings: SettingsRepository,
) : ProductActionHistoryRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    override fun observe(): Flow<List<ProductActionRecord>> =
        settings.observeValue<String>(STORAGE_KEY).map(::decodeDomain)

    override suspend fun upsert(record: ProductActionRecord) = mutex.withLock {
        val records = decode(settings.getValue<String>(STORAGE_KEY)).associateByTo(linkedMapOf(), ActionRecord::id)
        records[record.id] = ActionRecord.fromDomain(record)
        persist(records.values.sortedByDescending(ActionRecord::startedAtEpochMillis).take(MAX_RECORDS))
    }

    override suspend fun clearCompleted() = mutex.withLock {
        persist(
            decode(settings.getValue<String>(STORAGE_KEY))
                .filter { it.status == ProductActionStatus.RUNNING.name }
        )
    }

    private suspend fun persist(records: List<ActionRecord>) {
        settings.saveValue(STORAGE_KEY, records.let(json::encodeToString))
    }

    private fun decodeDomain(raw: String?): List<ProductActionRecord> =
        decode(raw).mapNotNull { record -> runCatching { record.toDomain() }.getOrNull() }

    private fun decode(raw: String?): List<ActionRecord> = raw?.let {
        runCatching { json.decodeFromString<List<ActionRecord>>(it) }.getOrDefault(emptyList())
    }.orEmpty()

    private companion object {
        const val STORAGE_KEY = "product_action_history_v1"
        const val MAX_RECORDS = 120
    }
}

@Serializable
private data class ActionRecord(
    val id: String,
    val type: String,
    val conflictKey: String,
    val label: String,
    val status: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long? = null,
    val sourceId: String? = null,
    val message: String? = null,
) {
    fun toDomain() = ProductActionRecord(
        id = id,
        type = ProductActionType.valueOf(type),
        conflictKey = conflictKey,
        label = label,
        status = ProductActionStatus.valueOf(status),
        startedAtEpochMillis = startedAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        sourceId = sourceId,
        message = message,
    )

    companion object {
        fun fromDomain(value: ProductActionRecord) = ActionRecord(
            id = value.id,
            type = value.type.name,
            conflictKey = value.conflictKey,
            label = value.label,
            status = value.status.name,
            startedAtEpochMillis = value.startedAtEpochMillis,
            finishedAtEpochMillis = value.finishedAtEpochMillis,
            sourceId = value.sourceId,
            message = value.message,
        )
    }
}
