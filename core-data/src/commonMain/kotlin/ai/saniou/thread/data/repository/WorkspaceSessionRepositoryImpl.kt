package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.WorkspaceSessionRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkspaceSessionRepositoryImpl(
    private val settings: SettingsRepository,
) : WorkspaceSessionRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val updateMutex = Mutex()

    override fun observe(): Flow<WorkspaceSession> =
        settings.observeValue<String>(STORAGE_KEY).map(::decode)

    override suspend fun get(): WorkspaceSession = decode(settings.getValue<String>(STORAGE_KEY))

    override suspend fun save(session: WorkspaceSession) {
        val normalized = session.copy(
            version = WorkspaceSession.CURRENT_VERSION,
            globalSearchQuery = session.globalSearchQuery.trim().take(MAX_QUERY_LENGTH),
        )
        settings.saveValue(STORAGE_KEY, json.encodeToString(PersistedSession.fromDomain(normalized)))
    }

    override suspend fun update(transform: (WorkspaceSession) -> WorkspaceSession) = updateMutex.withLock {
        save(transform(get()))
    }

    private fun decode(raw: String?): WorkspaceSession {
        if (raw.isNullOrBlank()) return WorkspaceSession()
        return runCatching {
            json.decodeFromString<PersistedSession>(raw).toDomain()
        }.getOrDefault(WorkspaceSession())
    }

    private companion object {
        const val STORAGE_KEY = "workspace_session_v1"
        const val MAX_QUERY_LENGTH = 240
    }
}

@Serializable
private data class PersistedSession(
    val version: Int = 1,
    val destination: String = "forum",
    val forumSourceId: String? = null,
    val globalSearchQuery: String = "",
    val updatedAtEpochMillis: Long = 0,
) {
    fun toDomain() = WorkspaceSession(
        version = WorkspaceSession.CURRENT_VERSION,
        destination = WorkspaceDestination.fromKey(destination),
        forumSourceId = forumSourceId?.takeIf(String::isNotBlank),
        globalSearchQuery = globalSearchQuery.take(240),
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    companion object {
        fun fromDomain(value: WorkspaceSession) = PersistedSession(
            version = value.version,
            destination = value.destination.key,
            forumSourceId = value.forumSourceId,
            globalSearchQuery = value.globalSearchQuery,
            updatedAtEpochMillis = value.updatedAtEpochMillis,
        )
    }
}
