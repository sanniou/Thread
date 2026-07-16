package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.InboxEventEntity
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.inbox.InboxEvent
import ai.saniou.thread.domain.model.inbox.InboxFilter
import ai.saniou.thread.domain.model.inbox.InboxKind
import ai.saniou.thread.domain.model.inbox.InboxSourceCount
import ai.saniou.thread.domain.model.inbox.InboxSummary
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.InboxRepository
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class InboxRepositoryImpl(
    private val db: Database,
) : InboxRepository {
    override fun getInbox(filter: InboxFilter): Flow<PagingData<InboxEvent>> = Pager(
        config = threadPagingConfig(),
        pagingSourceFactory = {
            QueryPagingSource(
                transacter = db.inboxEventQueries,
                context = Dispatchers.Default,
                countQuery = db.inboxEventQueries.countInbox(
                    unreadOnly = filter.unreadOnly.asLong(),
                    sourceIdFilter = filter.sourceId,
                    kindFilter = filter.kind?.name,
                    includeMuted = filter.includeMuted.asLong(),
                    query = filter.query.trim(),
                ),
                queryProvider = { limit, offset ->
                    db.inboxEventQueries.getInboxPaging(
                        unreadOnly = filter.unreadOnly.asLong(),
                        sourceIdFilter = filter.sourceId,
                        kindFilter = filter.kind?.name,
                        includeMuted = filter.includeMuted.asLong(),
                        query = filter.query.trim(),
                        limit = limit,
                        offset = offset,
                    )
                },
            )
        },
    ).flow.map { data -> data.map(InboxEventEntity::toDomain) }

    override fun observeSummary(): Flow<InboxSummary> = combine(
        db.inboxEventQueries.getInboxSummary().asFlow().mapToOne(ioDispatcher),
        db.inboxEventQueries.getInboxSourceCounts().asFlow().mapToList(ioDispatcher),
    ) { summary, sources ->
        InboxSummary(
            total = summary.totalCount.toInt(),
            unread = summary.unreadCount.toInt(),
            muted = summary.mutedCount.toInt(),
            sourceCounts = sources.map { row ->
                InboxSourceCount(
                    sourceId = row.sourceId,
                    total = row.totalCount.toInt(),
                    unread = row.unreadCount.toInt(),
                    muted = row.muted != 0L,
                )
            },
        )
    }

    override suspend fun upsert(event: InboxEvent) = withContext(ioDispatcher) {
        val sourceMuted = db.inboxEventQueries.getInboxSourcePreference(event.sourceId)
            .executeAsOneOrNull()?.muted == 1L
        val reference = event.reference
        db.inboxEventQueries.upsertInboxEvent(
            id = event.id,
            kind = event.kind.name,
            sourceId = event.sourceId,
            title = event.title,
            summary = event.summary,
            contentKind = reference?.kind?.name,
            contentId = reference?.id,
            contentSourceId = reference?.sourceId,
            parentId = reference?.parentId,
            canonicalUrl = reference?.canonicalUrl,
            occurredAt = event.occurredAt.toEpochMilliseconds(),
            readAt = event.readAt?.toEpochMilliseconds(),
            muted = (event.muted || sourceMuted).asLong(),
            priority = event.priority.toLong(),
        )
        Unit
    }

    override suspend fun markRead(id: String, read: Boolean) = withContext(ioDispatcher) {
        db.inboxEventQueries.markInboxRead(
            readAt = Clock.System.now().toEpochMilliseconds().takeIf { read },
            id = id,
        )
        Unit
    }

    override suspend fun markAllRead(filter: InboxFilter) = withContext(ioDispatcher) {
        db.inboxEventQueries.markAllInboxRead(
            readAt = Clock.System.now().toEpochMilliseconds(),
            sourceIdFilter = filter.sourceId,
            kindFilter = filter.kind?.name,
            includeMuted = filter.includeMuted.asLong(),
            query = filter.query.trim(),
        )
        Unit
    }

    override suspend fun setSourceMuted(sourceId: String, muted: Boolean) = withContext(ioDispatcher) {
        require(sourceId.isNotBlank())
        db.transaction {
            db.inboxEventQueries.upsertInboxSourcePreference(
                sourceId = sourceId,
                muted = muted.asLong(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
            db.inboxEventQueries.setInboxSourceMuted(muted.asLong(), sourceId)
        }
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        db.inboxEventQueries.deleteInboxEvent(id)
        Unit
    }
}

private fun InboxEventEntity.toDomain(): InboxEvent {
    val reference = contentKind?.let { kind ->
        contentId?.let { contentId ->
            runCatching {
                ContentReference(
                    kind = ContentReferenceKind.valueOf(kind),
                    id = contentId,
                    sourceId = contentSourceId,
                    parentId = parentId,
                    canonicalUrl = canonicalUrl,
                )
            }.getOrNull()
        }
    }
    return InboxEvent(
        id = id,
        kind = runCatching { InboxKind.valueOf(kind) }.getOrDefault(InboxKind.SYSTEM),
        sourceId = sourceId,
        title = title,
        summary = summary,
        reference = reference,
        occurredAt = Instant.fromEpochMilliseconds(occurredAt),
        readAt = readAt?.let(Instant::fromEpochMilliseconds),
        muted = muted != 0L,
        priority = priority.toInt().coerceIn(0, 3),
    )
}

private fun Boolean.asLong() = if (this) 1L else 0L
