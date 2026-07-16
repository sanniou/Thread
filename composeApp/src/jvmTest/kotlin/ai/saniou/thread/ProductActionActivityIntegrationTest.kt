package ai.saniou.thread

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.activity.ActivityState
import ai.saniou.thread.domain.model.activity.ActivityKind
import ai.saniou.thread.domain.model.activity.ProductActionConflictException
import ai.saniou.thread.domain.model.activity.ProductActionRecord
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionStatus
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import ai.saniou.thread.domain.repository.ActivityCenterRepository
import ai.saniou.thread.domain.repository.PostDraftRepository
import ai.saniou.thread.domain.repository.ProductActionExecutor
import ai.saniou.thread.domain.repository.ProductActionHistoryRepository
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductActionActivityIntegrationTest {
    @Test
    fun conflictsAreRejectedAndInterruptedWorkBecomesRetryableAfterRestart() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val firstDi = createAppDi(database)
        val executor = firstDi.direct.instance<ProductActionExecutor>()
        val export = ProductActionRequest(ProductActionType.EXPORT_USER_DATA)

        val first = async(start = CoroutineStart.UNDISPATCHED) { executor.execute(export) }
        val conflicting = executor.execute(export)
        assertIs<ProductActionConflictException>(conflicting.exceptionOrNull())
        assertTrue(first.await().isSuccess)

        val draftKey = PostDraftKey("nmb", PostDraftTargetKind.TOPIC, "topic-activity")
        firstDi.direct.instance<PostDraftRepository>().save(
            SavedPostDraft(
                key = draftKey,
                draft = PostDraft(content = "需要在重启后继续的回复"),
                updatedAtEpochMillis = 42,
            )
        )
        val orphanKey = PostDraftKey("removed-source", PostDraftTargetKind.CHANNEL, "lost-channel")
        firstDi.direct.instance<PostDraftRepository>().save(
            SavedPostDraft(
                key = orphanKey,
                draft = PostDraft(content = "来源删除后仍需保留"),
                updatedAtEpochMillis = 43,
            )
        )
        firstDi.direct.instance<ProductActionHistoryRepository>().upsert(
            ProductActionRecord(
                id = "interrupted-transfer",
                type = ProductActionType.RESTORE_FROM_WEBDAV,
                conflictKey = "user-data",
                label = "从 WebDAV 恢复",
                status = ProductActionStatus.RUNNING,
                startedAtEpochMillis = 41,
            )
        )

        val restartedDi = createAppDi(database)
        val restartedDrafts = withTimeout(10_000) {
            restartedDi.direct.instance<PostDraftRepository>().observeAll().first { it.isNotEmpty() }
        }
        assertEquals(setOf(draftKey, orphanKey), restartedDrafts.mapTo(mutableSetOf()) { it.key })

        val activity = withTimeout(10_000) {
            restartedDi.direct.instance<ActivityCenterRepository>().observe().first { snapshot ->
                snapshot.items.any { it.id == "action:interrupted-transfer" && it.state == ActivityState.FAILED } &&
                    snapshot.items.any { it.id == "draft:${draftKey.stableKey}" }
            }
        }
        assertTrue(activity.items.first { it.id == "action:interrupted-transfer" }.summary.contains("安全重试"))
        assertEquals(2, activity.drafts.size)
        assertEquals(null, activity.items.first { it.id == "draft:${orphanKey.stableKey}" }.deepLink)
    }

    @Test
    fun mixedActivityTimelineRemainsBoundedWithManyDraftsAndJobs() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val di = createAppDi(database)
        val drafts = di.direct.instance<PostDraftRepository>()
        val history = di.direct.instance<ProductActionHistoryRepository>()

        repeat(64) { index ->
            drafts.save(
                SavedPostDraft(
                    key = PostDraftKey("nmb", PostDraftTargetKind.TOPIC, "bulk-$index"),
                    draft = PostDraft(content = "mixed timeline draft $index"),
                    updatedAtEpochMillis = index.toLong() + 1,
                )
            )
        }
        repeat(90) { index ->
            history.upsert(
                ProductActionRecord(
                    id = "bulk-action-$index",
                    type = if (index % 2 == 0) ProductActionType.EXPORT_DIAGNOSTIC else ProductActionType.EXPORT_USER_DATA,
                    conflictKey = "bulk-$index",
                    label = "Bulk action $index",
                    status = if (index % 5 == 0) ProductActionStatus.FAILED else ProductActionStatus.SUCCEEDED,
                    startedAtEpochMillis = 1_000L + index,
                    finishedAtEpochMillis = 2_000L + index,
                    message = "bounded result $index",
                )
            )
        }

        val snapshot = withTimeout(15_000) {
            di.direct.instance<ActivityCenterRepository>().observe().first {
                it.drafts.size == 64 && it.actionRecords.size == 90
            }
        }
        assertEquals(64, snapshot.draftCount)
        assertEquals(40, snapshot.items.count { it.id.startsWith("action:") })
        assertEquals(64, snapshot.items.count { it.kind == ActivityKind.DRAFT })
        assertTrue(snapshot.items.takeWhile { it.state != ActivityState.COMPLETED }.isNotEmpty())
    }
}
