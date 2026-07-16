package ai.saniou.thread.data.source.runtime

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.cache.SqlDelightSourceCache
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.source.SourceDescriptor
import ai.saniou.thread.domain.model.source.SourceType
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.repository.Source
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DefaultSourceCatalogTest {
    @Test
    fun persistsAddsDisablesAndRemovesRuntimeInstances() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val builtIn = SourceDescriptor("nmb", SourceType.NMB, "NMB", isBuiltIn = true)
        val factory = FakeFactory()
        val catalog = DefaultSourceCatalog(
            database = database,
            builtIns = listOf(builtIn to RuntimeSourceRegistration(FakeSource("nmb", "NMB"))),
            factories = setOf(factory),
            defaults = listOf(builtIn),
        )
        val runtime = SourceDescriptor(
            id = "tech_forum",
            type = SourceType.DISCOURSE,
            displayName = "Tech Forum",
            baseUrl = "https://forum.example/",
        )

        catalog.upsert(runtime)
        assertEquals("Tech Forum", catalog.source("tech_forum")?.name)
        database.keyValueQueries.insertKeyValue("cookie_store_tech_forum", "old-site-cookie")
        catalog.upsert(runtime.copy(baseUrl = "https://forum-2.example/"))
        assertNull(database.keyValueQueries.getKeyValue("cookie_store_tech_forum").executeAsOneOrNull())
        catalog.setEnabled("tech_forum", false)
        assertNull(catalog.source("tech_forum"))
        assertTrue(factory.disposed >= 2)
        catalog.setEnabled("tech_forum", true)

        val restored = DefaultSourceCatalog(
            database = database,
            builtIns = listOf(builtIn to RuntimeSourceRegistration(FakeSource("nmb", "NMB"))),
            factories = setOf(factory),
            defaults = listOf(builtIn),
        )
        withTimeout(2_000) {
            restored.descriptors.first { values -> values.any { it.id == "tech_forum" } }
        }
        assertEquals("Tech Forum", restored.source("tech_forum")?.name)
        val cache = SqlDelightSourceCache(database)
        val author = Author("author", "Author", sourceName = "Tech Forum")
        cache.saveTopic(
            Topic(
                id = "cached-topic",
                channelId = "cached-channel",
                channelName = "Cached",
                title = "Cached topic",
                content = "body",
                summary = null,
                author = author,
                createdAt = Instant.fromEpochMilliseconds(1),
                commentCount = 1,
                images = listOf(Image("https://forum.example/topic.png", "https://forum.example/topic.png")),
                sourceId = "tech_forum",
                sourceName = "Tech Forum",
                sourceUrl = "https://forum.example/t/cached-topic",
            )
        )
        cache.saveComments(
            comments = listOf(
                Comment(
                    id = "cached-comment",
                    topicId = "cached-topic",
                    author = author,
                    createdAt = Instant.fromEpochMilliseconds(2),
                    title = null,
                    content = "reply",
                    images = listOf(Image("https://forum.example/reply.png", "https://forum.example/reply.png")),
                    isAdmin = false,
                    floor = 2,
                    sourceId = "tech_forum",
                )
            ),
            sourceId = "tech_forum",
            topicId = "cached-topic",
            viewMode = "all",
            page = 1,
            receiveDate = 2,
            startOrder = 0,
        )
        restored.remove("tech_forum")
        assertNull(restored.descriptors.value.firstOrNull { it.id == "tech_forum" })
        assertNull(database.topicQueries.getTopic("tech_forum", "cached-topic").executeAsOneOrNull())
        assertNull(database.commentQueries.getCommentById("tech_forum", "cached-comment").executeAsOneOrNull())
        assertEquals(
            emptyList(),
            database.imageQueries.getImagesByParent("tech_forum", "cached-topic", ImageType.Topic).executeAsList(),
        )
        assertEquals(
            emptyList(),
            database.imageQueries.getImagesByParent("tech_forum", "cached-comment", ImageType.Comment).executeAsList(),
        )
        assertTrue(factory.created >= 3)
        assertTrue(factory.disposed >= 2)
        driver.close()
    }
}

private class FakeFactory : RuntimeSourceFactory {
    override val type = SourceType.DISCOURSE
    var created = 0
    var disposed = 0

    override fun create(descriptor: SourceDescriptor): RuntimeSourceRegistration {
        created++
        return RuntimeSourceRegistration(
            source = FakeSource(descriptor.id, descriptor.displayName),
            dispose = { disposed++ },
        )
    }
}

private class FakeSource(
    override val id: String,
    override val name: String,
) : Source {
    override val isInitialized: Flow<Boolean> = flowOf(true)
    override val loginStrategy: LoginStrategy = LoginStrategy.Api("Login")
    override fun observeChannels(): Flow<List<Channel>> = flowOf(emptyList())
    override suspend fun fetchChannels(): Result<Unit> = Result.success(Unit)
    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> =
        Result.failure(UnsupportedOperationException())
    override fun getChannel(channelId: String): Flow<Channel?> = flowOf(null)
}
