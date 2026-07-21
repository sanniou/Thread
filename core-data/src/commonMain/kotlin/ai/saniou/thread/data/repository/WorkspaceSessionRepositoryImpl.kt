package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.model.workspace.FeedWorkspaceState
import ai.saniou.thread.domain.model.workspace.ForumWorkspaceState
import ai.saniou.thread.domain.model.workspace.ListAnchor
import ai.saniou.thread.domain.model.workspace.ReaderWorkspaceState
import ai.saniou.thread.domain.model.workspace.RestorableContentKind
import ai.saniou.thread.domain.model.workspace.RestorableContentReference
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
            forumSourceId = session.forum.sourceId ?: session.forumSourceId,
            forum = session.forum.copy(sourceId = session.forum.sourceId ?: session.forumSourceId),
            reader = session.reader.copy(
                searchQuery = session.reader.searchQuery.take(MAX_QUERY_LENGTH),
                articleFilter = session.reader.articleFilter.take(MAX_FILTER_LENGTH),
            ),
            feed = session.feed.copy(
                selectedSourceIds = session.feed.selectedSourceIds.take(MAX_SOURCE_COUNT).toSet(),
                selectedSocialSourceIds = session.feed.selectedSocialSourceIds.take(MAX_SOURCE_COUNT).toSet(),
            ),
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
        const val MAX_FILTER_LENGTH = 40
        const val MAX_SOURCE_COUNT = 100
    }
}

@Serializable
private data class PersistedSession(
    val version: Int = 1,
    val destination: String = "forum",
    val forumSourceId: String? = null,
    val globalSearchQuery: String = "",
    val activeSmartCollectionId: String? = null,
    val forum: PersistedForumState = PersistedForumState(),
    val reader: PersistedReaderState = PersistedReaderState(),
    val feed: PersistedFeedState = PersistedFeedState(),
    val lastContent: PersistedContentReference? = null,
    val updatedAtEpochMillis: Long = 0,
) {
    fun toDomain(): WorkspaceSession {
        val sourceId = forum.sourceId?.takeIf(String::isNotBlank) ?: forumSourceId?.takeIf(String::isNotBlank)
        return WorkspaceSession(
            version = WorkspaceSession.CURRENT_VERSION,
            destination = WorkspaceDestination.fromKey(destination),
            forumSourceId = sourceId,
            globalSearchQuery = globalSearchQuery.take(240),
            activeSmartCollectionId = activeSmartCollectionId?.takeIf(String::isNotBlank),
            forum = forum.toDomain().copy(sourceId = sourceId),
            reader = reader.toDomain(),
            feed = feed.toDomain(),
            lastContent = lastContent?.toDomain(),
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    companion object {
        fun fromDomain(value: WorkspaceSession) = PersistedSession(
            version = value.version,
            destination = value.destination.key,
            forumSourceId = value.forumSourceId,
            globalSearchQuery = value.globalSearchQuery,
            activeSmartCollectionId = value.activeSmartCollectionId,
            forum = PersistedForumState.fromDomain(value.forum),
            reader = PersistedReaderState.fromDomain(value.reader),
            feed = PersistedFeedState.fromDomain(value.feed),
            lastContent = value.lastContent?.let(PersistedContentReference::fromDomain),
            updatedAtEpochMillis = value.updatedAtEpochMillis,
        )
    }
}

@Serializable
private data class PersistedListAnchor(
    val contextKey: String = "",
    val index: Int = 0,
    val offset: Int = 0,
) {
    fun toDomain(): ListAnchor? = runCatching { ListAnchor(contextKey, index, offset) }.getOrNull()

    companion object {
        fun fromDomain(value: ListAnchor) = PersistedListAnchor(value.contextKey, value.index, value.offset)
    }
}

@Serializable
private data class PersistedForumState(
    val sourceId: String? = null,
    val channelId: String? = null,
    val expandedGroupId: String? = null,
    val listAnchor: PersistedListAnchor? = null,
) {
    fun toDomain() = ForumWorkspaceState(
        sourceId = sourceId?.takeIf(String::isNotBlank),
        channelId = channelId?.takeIf(String::isNotBlank),
        expandedGroupId = expandedGroupId?.takeIf(String::isNotBlank),
        listAnchor = listAnchor?.toDomain(),
    )

    companion object {
        fun fromDomain(value: ForumWorkspaceState) = PersistedForumState(
            value.sourceId,
            value.channelId,
            value.expandedGroupId,
            value.listAnchor?.let(PersistedListAnchor::fromDomain),
        )
    }
}

@Serializable
private data class PersistedReaderState(
    val feedSourceId: String? = null,
    val articleFilter: String = "ALL",
    val searchQuery: String = "",
    val previewArticleId: String? = null,
    val listAnchor: PersistedListAnchor? = null,
) {
    fun toDomain() = ReaderWorkspaceState(
        feedSourceId = feedSourceId?.takeIf(String::isNotBlank),
        articleFilter = articleFilter,
        searchQuery = searchQuery.take(240),
        previewArticleId = previewArticleId?.takeIf(String::isNotBlank),
        listAnchor = listAnchor?.toDomain(),
    )

    companion object {
        fun fromDomain(value: ReaderWorkspaceState) = PersistedReaderState(
            value.feedSourceId,
            value.articleFilter,
            value.searchQuery,
            value.previewArticleId,
            value.listAnchor?.let(PersistedListAnchor::fromDomain),
        )
    }
}

@Serializable
private data class PersistedFeedState(
    val selectedSourceIds: Set<String> = emptySet(),
    val hasExplicitSourceSelection: Boolean = false,
    val includeReader: Boolean = true,
    val selectedSocialSourceIds: Set<String> = emptySet(),
    val hasExplicitSocialSourceSelection: Boolean = false,
    val includeSocial: Boolean = true,
    val listAnchor: PersistedListAnchor? = null,
) {
    fun toDomain() = FeedWorkspaceState(
        selectedSourceIds = selectedSourceIds.filter(String::isNotBlank).take(100).toSet(),
        hasExplicitSourceSelection = hasExplicitSourceSelection,
        includeReader = includeReader,
        selectedSocialSourceIds = selectedSocialSourceIds.filter(String::isNotBlank).take(100).toSet(),
        hasExplicitSocialSourceSelection = hasExplicitSocialSourceSelection,
        includeSocial = includeSocial,
        listAnchor = listAnchor?.toDomain(),
    )

    companion object {
        fun fromDomain(value: FeedWorkspaceState) = PersistedFeedState(
            value.selectedSourceIds,
            value.hasExplicitSourceSelection,
            value.includeReader,
            value.selectedSocialSourceIds,
            value.hasExplicitSocialSourceSelection,
            value.includeSocial,
            value.listAnchor?.let(PersistedListAnchor::fromDomain),
        )
    }
}

@Serializable
private data class PersistedContentReference(
    val kind: String,
    val id: String,
    val sourceId: String? = null,
    val workspace: String,
) {
    fun toDomain(): RestorableContentReference? = runCatching {
        RestorableContentReference(
            kind = RestorableContentKind.valueOf(kind),
            id = id,
            sourceId = sourceId,
            workspace = WorkspaceDestination.fromKey(workspace),
        )
    }.getOrNull()

    companion object {
        fun fromDomain(value: RestorableContentReference) = PersistedContentReference(
            value.kind.name,
            value.id,
            value.sourceId,
            value.workspace.key,
        )
    }
}
