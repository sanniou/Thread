package ai.saniou.thread.data.repository

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.inbox.InboxEvent
import ai.saniou.thread.domain.model.inbox.InboxFilter
import ai.saniou.thread.domain.model.inbox.InboxKind
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class InboxRepositoryImplTest {
    @Test
    fun largeMixedInboxSupportsCountsMuteAndBoundedPaging() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val repository = InboxRepositoryImpl(database)

        repeat(240) { index ->
            val source = if (index % 2 == 0) "reader-a" else "forum-b"
            repository.upsert(
                InboxEvent(
                    id = "event-$index",
                    kind = if (index % 3 == 0) InboxKind.READER_UPDATE else InboxKind.REPLY,
                    sourceId = source,
                    title = "Update $index",
                    summary = "A searchable notification number $index",
                    reference = ContentReference(
                        kind = if (source.startsWith("reader")) ContentReferenceKind.ARTICLE else ContentReferenceKind.TOPIC,
                        id = index.toString(),
                        sourceId = source.takeUnless { it.startsWith("reader") },
                    ),
                    occurredAt = Instant.fromEpochMilliseconds(index.toLong()),
                )
            )
        }

        val initial = repository.observeSummary().first { it.total == 240 }
        assertEquals(240, initial.unread)
        assertEquals(2, initial.sourceCounts.size)

        repository.markAllRead(InboxFilter(sourceId = "reader-a"))
        val halfRead = repository.observeSummary().first { it.unread == 120 }
        assertEquals(120, halfRead.sourceCounts.first { it.sourceId == "reader-a" }.total)

        repository.setSourceMuted("forum-b", true)
        val muted = repository.observeSummary().first { it.muted == 120 }
        assertEquals(0, muted.unread)
        assertTrue(muted.sourceCounts.first { it.sourceId == "forum-b" }.muted)

        repository.upsert(
            InboxEvent(
                id = "future",
                kind = InboxKind.SYSTEM,
                sourceId = "forum-b",
                title = "Future",
                summary = "inherits preference",
                reference = null,
                occurredAt = Instant.fromEpochMilliseconds(999),
            )
        )
        assertEquals(1L, database.inboxEventQueries.getInboxEvent("future").executeAsOne().muted)

        val page = database.inboxEventQueries.getInboxPaging(
            unreadOnly = 0,
            sourceIdFilter = null,
            kindFilter = null,
            includeMuted = 1,
            query = "",
            limit = 40,
            offset = 200,
        ).executeAsList()
        assertEquals(40, page.size)
        assertFalse(page.map { it.id }.contains("future"))
        driver.close()
    }
}
