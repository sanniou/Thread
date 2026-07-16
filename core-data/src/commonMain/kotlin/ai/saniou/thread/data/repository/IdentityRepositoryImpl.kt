package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.activity.IdentityLoginKind
import ai.saniou.thread.domain.model.activity.IdentityValidity
import ai.saniou.thread.domain.model.activity.SourceIdentityStatus
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.refresh.DiagnosticSanitizer
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.IdentityRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.domain.source.SourceCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class IdentityRepositoryImpl(
    private val sourceCatalog: SourceCatalog,
    private val accountRepository: AccountRepository,
    private val settings: SettingsRepository,
) : IdentityRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    override fun observe(): Flow<List<SourceIdentityStatus>> = combine(
        sourceCatalog.descriptors,
        accountRepository.getCurrentAccounts(),
        settings.observeValue<String>(STORAGE_KEY),
    ) { descriptors, accounts, raw ->
        val records = decode(raw).associateBy(IdentityRecord::sourceId)
        val accountBySource = accounts.associateBy { it.sourceId }
        descriptors.map { descriptor ->
            val source = sourceCatalog.source(descriptor.id)
            val supportsLogin = source?.capabilities?.supportsLogin == true
            val account = accountBySource[descriptor.id]
            val record = records[descriptor.id]
            val validity = when {
                !descriptor.enabled -> IdentityValidity.DISABLED
                !supportsLogin -> IdentityValidity.NOT_APPLICABLE
                record?.validity == IdentityValidity.EXPIRED.name -> IdentityValidity.EXPIRED
                account != null -> IdentityValidity.VALID
                else -> IdentityValidity.ANONYMOUS
            }
            SourceIdentityStatus(
                sourceId = descriptor.id,
                sourceName = descriptor.displayName,
                validity = validity,
                loginKind = source?.loginStrategy.toLoginKind(supportsLogin),
                supportsLogin = supportsLogin,
                accountId = account?.id,
                accountAlias = account?.alias ?: account?.uid,
                accountAvatar = account?.avatar,
                lastValidatedAtEpochMillis = record?.changedAtEpochMillis ?: account?.lastUsedAt?.toEpochMilliseconds(),
                message = record?.message,
            )
        }.sortedBy(SourceIdentityStatus::sourceName)
    }

    override suspend fun markAuthenticated(sourceId: String, validatedAtEpochMillis: Long) {
        update(sourceId) { IdentityRecord(sourceId, IdentityValidity.VALID.name, validatedAtEpochMillis) }
    }

    override suspend fun markExpired(sourceId: String, message: String?, detectedAtEpochMillis: Long) {
        update(sourceId) {
            IdentityRecord(
                sourceId = sourceId,
                validity = IdentityValidity.EXPIRED.name,
                changedAtEpochMillis = detectedAtEpochMillis,
                message = DiagnosticSanitizer.sanitize(message)?.take(500),
            )
        }
    }

    override suspend fun clear(sourceId: String) {
        update(sourceId) { null }
    }

    private suspend fun update(sourceId: String, transform: (IdentityRecord?) -> IdentityRecord?) = mutex.withLock {
        val records = decode(settings.getValue<String>(STORAGE_KEY)).associateByTo(linkedMapOf(), IdentityRecord::sourceId)
        val next = transform(records[sourceId])
        if (next == null) records.remove(sourceId) else records[sourceId] = next
        settings.saveValue(
            STORAGE_KEY,
            records.values.sortedByDescending(IdentityRecord::changedAtEpochMillis).let(json::encodeToString),
        )
    }

    private fun decode(raw: String?): List<IdentityRecord> = raw?.let {
        runCatching { json.decodeFromString<List<IdentityRecord>>(it) }.getOrDefault(emptyList())
    }.orEmpty()

    private fun LoginStrategy?.toLoginKind(supportsLogin: Boolean): IdentityLoginKind = when {
        !supportsLogin -> IdentityLoginKind.NONE
        this is LoginStrategy.Manual -> IdentityLoginKind.MANUAL
        this is LoginStrategy.WebView -> IdentityLoginKind.WEB
        this is LoginStrategy.Api -> IdentityLoginKind.API
        else -> IdentityLoginKind.NONE
    }

    private companion object {
        const val STORAGE_KEY = "source_identity_status_v1"
    }
}

@Serializable
private data class IdentityRecord(
    val sourceId: String,
    val validity: String,
    val changedAtEpochMillis: Long,
    val message: String? = null,
)
