package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.storage.getStorageDirectory
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetForumCommentCacheStats
import ai.saniou.thread.db.table.forum.GetForumTopicCacheStats
import ai.saniou.thread.db.table.reader.GetReaderCacheStats
import ai.saniou.thread.domain.model.SourceCapabilities
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.DiagnosticExport
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.DiagnosticSanitizer
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshHistory
import ai.saniou.thread.domain.refresh.RefreshHistoryRepository
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.refresh.RefreshTaskState
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.source.SourceCatalog
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class OperationsRepositoryImpl(
    private val database: Database,
    private val sourceCatalog: SourceCatalog,
    private val readerRepository: ReaderRepository,
    private val refreshCoordinator: RefreshCoordinator,
    private val refreshHistoryRepository: RefreshHistoryRepository,
) : OperationsRepository {
    private val diagnosticJson = Json { prettyPrint = true; encodeDefaults = true }

    override fun observe(): Flow<OperationsSnapshot> {
        val forumInputs = combine(
        sourceCatalog.descriptors,
        database.topicQueries.getForumTopicCacheStats().asFlow().mapToList(ioDispatcher),
        database.commentQueries.getForumCommentCacheStats().asFlow().mapToList(ioDispatcher),
        ) { descriptors, topics, comments -> ForumInputs(descriptors, topics, comments) }
        val readerInputs = combine(
        readerRepository.getAllFeedSources(),
        database.articleQueries.getReaderCacheStats().asFlow().mapToList(ioDispatcher),
        ) { sources, stats -> ReaderInputs(sources, stats) }
        return combine(
        forumInputs,
        readerInputs,
        refreshCoordinator.states,
        refreshHistoryRepository.observe(),
    ) { forumInputsValue, readerInputsValue, tasks, histories ->
        val now = Clock.System.now().toEpochMilliseconds()
        val descriptors = forumInputsValue.descriptors
        val topicStats = forumInputsValue.topics
        val commentStats = forumInputsValue.comments
        val readerSources = readerInputsValue.sources
        val readerStats = readerInputsValue.stats
        val topicBySource = topicStats.associateBy { it.sourceId }
        val commentsBySource = commentStats.associateBy { it.sourceId }
        val readerStatsBySource = readerStats.associateBy { it.sourceId }
        val forum = descriptors.map { descriptor ->
            val latest = latestTask(tasks.values, "forum:${descriptor.id}:")
            val history = latestHistory(histories.values, "forum:${descriptor.id}:")
            val topic = topicBySource[descriptor.id]
            SourceHealth(
                id = descriptor.id,
                name = descriptor.displayName,
                kind = ContentSourceKind.FORUM,
                state = operationalState(descriptor.enabled, latest, history, now),
                enabled = descriptor.enabled,
                primaryItemCount = topic?.topicCount ?: 0,
                secondaryItemCount = commentsBySource[descriptor.id]?.commentCount ?: 0,
                lastContentAtEpochMillis = topic?.lastContentAt?.takeIf { it > 0 },
                lastRefreshAtEpochMillis = maxOfNullable(latest?.finishedAtEpochMillis, history?.lastAttemptAtEpochMillis),
                lastSuccessfulRefreshAtEpochMillis = history?.lastSuccessAtEpochMillis,
                cacheAgeMillis = topic?.lastContentAt?.takeIf { it > 0 }?.let { (now - it).coerceAtLeast(0) },
                refreshAttempt = latest?.attempt ?: 0,
                consecutiveFailureCount = history?.consecutiveFailureCount ?: 0,
                rateLimitUntilEpochMillis = history?.rateLimitUntilEpochMillis,
                failureKind = latest?.failureKind ?: history?.failureKind,
                message = latest?.message ?: history?.message,
                capabilities = capabilities(descriptor),
            )
        }
        val reader = readerSources.map { source ->
            val stats = readerStatsBySource[source.id]
            val latest = tasks["reader:${source.id}"]
            val history = histories["reader:${source.id}"]
            SourceHealth(
                id = source.id,
                name = source.name,
                kind = ContentSourceKind.READER,
                state = operationalState(true, latest, history, now),
                enabled = true,
                primaryItemCount = stats?.articleCount ?: 0,
                secondaryItemCount = stats?.unreadCount ?: 0,
                lastContentAtEpochMillis = source.lastUpdate.takeIf { it > 0 },
                lastRefreshAtEpochMillis = maxOfNullable(latest?.finishedAtEpochMillis, history?.lastAttemptAtEpochMillis),
                lastSuccessfulRefreshAtEpochMillis = history?.lastSuccessAtEpochMillis,
                cacheAgeMillis = source.lastUpdate.takeIf { it > 0 }?.let { (now - it).coerceAtLeast(0) },
                refreshAttempt = latest?.attempt ?: 0,
                consecutiveFailureCount = history?.consecutiveFailureCount ?: 0,
                rateLimitUntilEpochMillis = history?.rateLimitUntilEpochMillis,
                failureKind = latest?.failureKind ?: history?.failureKind,
                message = latest?.message ?: history?.message,
                capabilities = buildSet {
                    add(source.type.name)
                    if (source.autoRefresh) add("自动刷新")
                },
            )
        }
        val sources = (forum + reader).sortedWith(compareBy(SourceHealth::kind, SourceHealth::name))
        OperationsSnapshot(
            sources = sources,
            activeRefreshCount = tasks.values.count { it.status == RefreshStatus.RUNNING },
            failedRefreshCount = sources.count { it.state in ATTENTION_STATES },
            cachedItemCount = forum.sumOf { it.primaryItemCount + it.secondaryItemCount } +
                reader.sumOf(SourceHealth::primaryItemCount),
            storageDirectory = getStorageDirectory(),
        )
    }
    }

    override suspend fun clearRefreshDiagnostic(sourceId: String) {
        refreshCoordinator.states.value.keys
            .filter { it == "reader:$sourceId" || it.startsWith("forum:$sourceId:") }
            .forEach(refreshCoordinator::clear)
        refreshHistoryRepository.clearSource(sourceId)
    }

    override suspend fun exportDiagnostic(): DiagnosticExport {
        val snapshot = observe().first()
        val generatedAt = Clock.System.now().toEpochMilliseconds()
        val payload = diagnosticJson.encodeToString(
            PersistedDiagnostic(
                generatedAtEpochMillis = generatedAt,
                cachedItemCount = snapshot.cachedItemCount,
                activeRefreshCount = snapshot.activeRefreshCount,
                failedRefreshCount = snapshot.failedRefreshCount,
                storageConfigured = snapshot.storageDirectory.isNotBlank(),
                sources = snapshot.sources.map { source ->
                    PersistedDiagnosticSource(
                        id = source.id.take(120),
                        name = DiagnosticSanitizer.sanitize(source.name)?.take(120).orEmpty(),
                        kind = source.kind.name,
                        state = source.state.name,
                        enabled = source.enabled,
                        primaryItemCount = source.primaryItemCount,
                        secondaryItemCount = source.secondaryItemCount,
                        lastContentAtEpochMillis = source.lastContentAtEpochMillis,
                        lastRefreshAtEpochMillis = source.lastRefreshAtEpochMillis,
                        lastSuccessfulRefreshAtEpochMillis = source.lastSuccessfulRefreshAtEpochMillis,
                        consecutiveFailureCount = source.consecutiveFailureCount,
                        failureKind = source.failureKind?.name,
                        rateLimitUntilEpochMillis = source.rateLimitUntilEpochMillis,
                        message = DiagnosticSanitizer.sanitize(source.message),
                        capabilities = source.capabilities.sorted().take(20),
                    )
                },
            )
        )
        return DiagnosticExport(payload, generatedAt, snapshot.sources.size)
    }

    private fun latestTask(tasks: Collection<RefreshTaskState>, prefix: String): RefreshTaskState? =
        tasks.filter { it.key.startsWith(prefix) }.maxByOrNull(RefreshTaskState::startedAtEpochMillis)

    private fun latestHistory(histories: Collection<RefreshHistory>, prefix: String): RefreshHistory? =
        histories.filter { it.key.startsWith(prefix) }.maxByOrNull(RefreshHistory::lastAttemptAtEpochMillis)

    private fun operationalState(
        enabled: Boolean,
        task: RefreshTaskState?,
        history: RefreshHistory?,
        now: Long,
    ): SourceOperationalState = when {
        !enabled -> SourceOperationalState.DISABLED
        task?.status == RefreshStatus.RUNNING -> SourceOperationalState.REFRESHING
        task?.status == RefreshStatus.SUCCEEDED -> SourceOperationalState.READY
        history?.rateLimitUntilEpochMillis?.let { it > now } == true -> SourceOperationalState.RATE_LIMITED
        (task?.failureKind ?: history?.failureKind) == RefreshFailureKind.OFFLINE -> SourceOperationalState.OFFLINE
        (task?.failureKind ?: history?.failureKind) == RefreshFailureKind.AUTHENTICATION -> SourceOperationalState.AUTHENTICATION_REQUIRED
        (task?.failureKind ?: history?.failureKind) == RefreshFailureKind.RATE_LIMIT &&
            history?.rateLimitUntilEpochMillis == null -> SourceOperationalState.RATE_LIMITED
        task?.status != RefreshStatus.FAILED &&
            (history == null || history.consecutiveFailureCount == 0) -> SourceOperationalState.READY
        else -> SourceOperationalState.DEGRADED
    }

    private fun capabilities(descriptor: SourceDescriptor): Set<String> {
        val value = sourceCatalog.source(descriptor.id)?.capabilities ?: SourceCapabilities.Default
        return buildSet {
            if (value.supportsChannelCatalog) add("版块")
            if (value.supportsSearch) add("搜索")
            if (value.supportsReplies) add("回复")
            if (value.supportsTopicCreation) add("发帖")
            if (value.supportsLogin) add("登录")
            if (value.hasSubComments) add("楼中楼")
            if (value.hasUpvote) add("互动")
        }
    }
}

private fun maxOfNullable(first: Long?, second: Long?): Long? = when {
    first == null -> second
    second == null -> first
    else -> maxOf(first, second)
}

private val ATTENTION_STATES = setOf(
    SourceOperationalState.OFFLINE,
    SourceOperationalState.AUTHENTICATION_REQUIRED,
    SourceOperationalState.RATE_LIMITED,
    SourceOperationalState.DEGRADED,
)

@Serializable
private data class PersistedDiagnostic(
    val formatVersion: Int = 1,
    val redacted: Boolean = true,
    val generatedAtEpochMillis: Long,
    val cachedItemCount: Long,
    val activeRefreshCount: Int,
    val failedRefreshCount: Int,
    val storageConfigured: Boolean,
    val sources: List<PersistedDiagnosticSource>,
)

@Serializable
private data class PersistedDiagnosticSource(
    val id: String,
    val name: String,
    val kind: String,
    val state: String,
    val enabled: Boolean,
    val primaryItemCount: Long,
    val secondaryItemCount: Long,
    val lastContentAtEpochMillis: Long? = null,
    val lastRefreshAtEpochMillis: Long? = null,
    val lastSuccessfulRefreshAtEpochMillis: Long? = null,
    val consecutiveFailureCount: Int,
    val failureKind: String? = null,
    val rateLimitUntilEpochMillis: Long? = null,
    val message: String? = null,
    val capabilities: List<String>,
)

private data class ForumInputs(
    val descriptors: List<SourceDescriptor>,
    val topics: List<GetForumTopicCacheStats>,
    val comments: List<GetForumCommentCacheStats>,
)

private data class ReaderInputs(
    val sources: List<FeedSource>,
    val stats: List<GetReaderCacheStats>,
)
