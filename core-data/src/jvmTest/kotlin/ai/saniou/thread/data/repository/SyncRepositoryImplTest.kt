package ai.saniou.thread.data.repository

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.sync.webdav.UserDataRemoteTransport
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import ai.saniou.thread.domain.model.sync.WebDavConfig
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.source.ForumSearchConnector
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.domain.source.PostingConnector
import ai.saniou.thread.domain.source.ReactionConnector
import ai.saniou.thread.domain.source.SourceCatalog
import ai.saniou.thread.domain.source.SubCommentConnector
import ai.saniou.thread.domain.source.UserContentConnector
import ai.saniou.thread.domain.source.UserRelationConnector
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncRepositoryImplTest {
    @Test
    fun backupAndRestoreAreVersionedAndIdempotent() = runBlocking {
        val origin = testDatabase()
        val target = testDatabase()
        val transport = MemoryTransport()
        val originRepository = SyncRepositoryImpl(origin.database, TestCatalog(), transport)
        val targetRepository = SyncRepositoryImpl(target.database, TestCatalog(), transport)
        val config = WebDavConfig("https://dav.example.com/thread.json", "dev", "test")

        origin.database.feedSourceQueries.insertFeedSource(
            id = "feed", name = "Feed", url = "https://example.com/rss", type = "RSS",
            description = null, iconUrl = null, lastUpdate = 10L,
            selectorConfig = "{}", autoRefresh = 1L, refreshInterval = 3_600_000L,
        )
        origin.database.bookmarkQueries.insert(
            Bookmark.Text("bookmark", Instant.fromEpochMilliseconds(20), emptyList(), "saved").toEntity(),
        )
        origin.database.articleQueries.upsertArticleUserState("article", 1L, 0L, 30L)
        origin.database.keyValueQueries.insertKeyValue("current_source_id", "nmb")
        originRepository.saveWebDavConfig(config)
        targetRepository.saveWebDavConfig(config)

        val export = originRepository.backupToWebDav().getOrThrow()
        assertTrue(export.payload.contains("thread-user-data"))
        assertEquals(1, export.summary.feedSourceCount)
        assertEquals(1, export.summary.bookmarkCount)

        val first = targetRepository.restoreFromWebDav().getOrThrow()
        val second = targetRepository.restoreFromWebDav().getOrThrow()

        assertEquals(first.summary, second.summary)
        assertNotNull(target.database.feedSourceQueries.getFeedSourceById("feed").executeAsOneOrNull())
        assertNotNull(target.database.bookmarkQueries.getById("bookmark").executeAsOneOrNull())
        val state = target.database.articleQueries.getArticleUserState("article").executeAsOne()
        assertEquals(1L, state.isRead)
        assertEquals(0L, state.isBookmarked)
        assertEquals("nmb", target.database.keyValueQueries.getKeyValue("current_source_id").executeAsOne().content)
        assertEquals(1L, target.database.feedSourceQueries.getAllFeedSources().executeAsList().size.toLong())

        origin.close()
        target.close()
    }

    private fun testDatabase(): TestDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        return TestDatabase(createDatabase(driver), driver)
    }
}

private data class TestDatabase(
    val database: Database,
    val driver: JdbcSqliteDriver,
) {
    fun close() = driver.close()
}

private class MemoryTransport : UserDataRemoteTransport {
    var payload: String? = null

    override suspend fun write(config: WebDavConfig, payload: String): Result<Unit> {
        this.payload = payload
        return Result.success(Unit)
    }

    override suspend fun read(config: WebDavConfig): Result<String> =
        payload?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No backup"))
}

private class TestCatalog : SourceCatalog {
    private val builtIn = SourceDescriptor("nmb", SourceType.NMB, "NMB", isBuiltIn = true)
    private val mutableDescriptors = MutableStateFlow(listOf(builtIn))
    override val descriptors: StateFlow<List<SourceDescriptor>> = mutableDescriptors
    override val availableSources: StateFlow<List<Source>> = MutableStateFlow(emptyList())

    override fun supports(type: SourceType) = type == SourceType.DISCOURSE
    override suspend fun upsert(descriptor: SourceDescriptor) {
        mutableDescriptors.value = mutableDescriptors.value.filterNot { it.id == descriptor.id } + descriptor
    }
    override suspend fun setEnabled(sourceId: String, enabled: Boolean) {
        mutableDescriptors.value = mutableDescriptors.value.map {
            if (it.id == sourceId) it.copy(enabled = enabled) else it
        }
    }
    override suspend fun remove(sourceId: String) {
        mutableDescriptors.value = mutableDescriptors.value.filterNot { it.id == sourceId }
    }

    override fun source(sourceId: String): Source? = null
    override fun search(sourceId: String): ForumSearchConnector? = null
    override fun userContent(sourceId: String): UserContentConnector? = null
    override fun posting(sourceId: String): PostingConnector? = null
    override fun login(sourceId: String): LoginConnector? = null
    override fun subComments(sourceId: String): SubCommentConnector? = null
    override fun reactions(sourceId: String): ReactionConnector? = null
    override fun userRelation(sourceId: String): UserRelationConnector? = null
}
