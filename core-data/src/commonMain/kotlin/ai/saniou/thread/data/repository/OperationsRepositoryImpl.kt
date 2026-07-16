package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.storage.getStorageDirectory
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetForumCommentCacheStats
import ai.saniou.thread.db.table.forum.GetForumTopicCacheStats
import ai.saniou.thread.db.table.reader.GetReaderCacheStats
import ai.saniou.thread.domain.model.SourceCapabilities
import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.refresh.RefreshTaskState
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.source.SourceCatalog
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OperationsRepositoryImpl(
    private val database: Database,
    private val sourceCatalog: SourceCatalog,
    private val readerRepository: ReaderRepository,
    private val refreshCoordinator: RefreshCoordinator,
) : OperationsRepository {
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
    ) { forumInputsValue, readerInputsValue, tasks ->
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
            val topic = topicBySource[descriptor.id]
            SourceHealth(
                id = descriptor.id,
                name = descriptor.displayName,
                kind = ContentSourceKind.FORUM,
                state = operationalState(descriptor.enabled, latest),
                enabled = descriptor.enabled,
                primaryItemCount = topic?.topicCount ?: 0,
                secondaryItemCount = commentsBySource[descriptor.id]?.commentCount ?: 0,
                lastContentAtEpochMillis = topic?.lastContentAt?.takeIf { it > 0 },
                lastRefreshAtEpochMillis = latest?.finishedAtEpochMillis,
                refreshAttempt = latest?.attempt ?: 0,
                failureKind = latest?.failureKind,
                message = latest?.message,
                capabilities = capabilities(descriptor),
            )
        }
        val reader = readerSources.map { source ->
            val stats = readerStatsBySource[source.id]
            val latest = tasks["reader:${source.id}"]
            SourceHealth(
                id = source.id,
                name = source.name,
                kind = ContentSourceKind.READER,
                state = operationalState(true, latest),
                enabled = true,
                primaryItemCount = stats?.articleCount ?: 0,
                secondaryItemCount = stats?.unreadCount ?: 0,
                lastContentAtEpochMillis = source.lastUpdate.takeIf { it > 0 },
                lastRefreshAtEpochMillis = latest?.finishedAtEpochMillis,
                refreshAttempt = latest?.attempt ?: 0,
                failureKind = latest?.failureKind,
                message = latest?.message,
                capabilities = buildSet {
                    add(source.type.name)
                    if (source.autoRefresh) add("自动刷新")
                },
            )
        }
        OperationsSnapshot(
            sources = (forum + reader).sortedWith(compareBy(SourceHealth::kind, SourceHealth::name)),
            activeRefreshCount = tasks.values.count { it.status == RefreshStatus.RUNNING },
            failedRefreshCount = tasks.values.count { it.status == RefreshStatus.FAILED },
            cachedItemCount = forum.sumOf { it.primaryItemCount + it.secondaryItemCount } +
                reader.sumOf(SourceHealth::primaryItemCount),
            storageDirectory = getStorageDirectory(),
        )
    }
    }

    override fun clearRefreshDiagnostic(sourceId: String) {
        refreshCoordinator.states.value.keys
            .filter { it == "reader:$sourceId" || it.startsWith("forum:$sourceId:") }
            .forEach(refreshCoordinator::clear)
    }

    private fun latestTask(tasks: Collection<RefreshTaskState>, prefix: String): RefreshTaskState? =
        tasks.filter { it.key.startsWith(prefix) }.maxByOrNull(RefreshTaskState::startedAtEpochMillis)

    private fun operationalState(enabled: Boolean, task: RefreshTaskState?): SourceOperationalState = when {
        !enabled -> SourceOperationalState.DISABLED
        task?.status == RefreshStatus.RUNNING -> SourceOperationalState.REFRESHING
        task?.status != RefreshStatus.FAILED -> SourceOperationalState.READY
        task.failureKind == RefreshFailureKind.OFFLINE -> SourceOperationalState.OFFLINE
        task.failureKind == RefreshFailureKind.AUTHENTICATION -> SourceOperationalState.AUTHENTICATION_REQUIRED
        task.failureKind == RefreshFailureKind.RATE_LIMIT -> SourceOperationalState.RATE_LIMITED
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

private data class ForumInputs(
    val descriptors: List<SourceDescriptor>,
    val topics: List<GetForumTopicCacheStats>,
    val comments: List<GetForumCommentCacheStats>,
)

private data class ReaderInputs(
    val sources: List<FeedSource>,
    val stats: List<GetReaderCacheStats>,
)
