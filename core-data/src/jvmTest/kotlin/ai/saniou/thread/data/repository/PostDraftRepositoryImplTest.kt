package ai.saniou.thread.data.repository

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.PostAttachment
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.PostDraftTargetKind
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostDraftRepositoryImplTest {
    @Test
    fun textOptionsAndAttachmentRoundTripThenDiscardExplicitly() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val repository = PostDraftRepositoryImpl(SettingsRepositoryImpl(createDatabase(driver)))
        val key = PostDraftKey("nmb", PostDraftTargetKind.TOPIC, "topic-alpha")
        val bytes = byteArrayOf(1, 3, 5, 7)

        repository.save(
            SavedPostDraft(
                key = key,
                draft = PostDraft(
                    content = "跨重启回复草稿",
                    name = "匿名用户",
                    title = "可选标题",
                    water = true,
                    attachment = PostAttachment("draft.png", bytes, "image/png"),
                ),
                updatedAtEpochMillis = 42,
            )
        )

        val restored = repository.get(key)!!
        assertEquals(SavedPostDraft.CURRENT_VERSION, restored.version)
        assertEquals("跨重启回复草稿", restored.draft.content)
        assertEquals("draft.png", restored.draft.attachment?.fileName)
        assertContentEquals(bytes, restored.draft.attachment?.bytes)
        assertEquals(42, restored.updatedAtEpochMillis)
        assertEquals(key, withTimeout(5_000) { repository.observeAll().first { it.isNotEmpty() } }.single().key)

        repository.discard(key)
        assertNull(repository.get(key))
        assertEquals(emptyList(), withTimeout(5_000) { repository.observeAll().first() })
        driver.close()
    }
}
