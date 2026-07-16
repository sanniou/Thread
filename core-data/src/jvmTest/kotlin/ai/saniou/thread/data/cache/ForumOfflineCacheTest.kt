package ai.saniou.thread.data.cache

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ForumOfflineCacheTest {
    @Test
    fun restoresRecentChannelsAndSubCommentImagesFromCommonDatabase() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val cache = SqlDelightSourceCache(database)

        database.channelQueries.insertChannel(channel("one").toEntity())
        database.channelQueries.insertChannel(channel("two").toEntity())
        database.channelQueries.upsertChannelVisit("tieba", "one", 10)
        database.channelQueries.upsertChannelVisit("tieba", "two", 20)
        assertEquals(
            listOf("two", "one"),
            database.channelQueries.getRecentChannels("tieba", 5).executeAsList().map { it.id },
        )

        val author = Author("author", "Author", sourceName = "Tieba")
        cache.saveTopic(
            Topic(
                id = "topic",
                channelId = "one",
                channelName = "One",
                title = "Topic",
                content = "body",
                summary = null,
                author = author,
                createdAt = Instant.fromEpochMilliseconds(1),
                commentCount = 1,
                sourceId = "tieba",
                sourceName = "Tieba",
                sourceUrl = "https://example.test/topic",
            )
        )
        val child = Comment(
            id = "child",
            topicId = "topic",
            author = author,
            createdAt = Instant.fromEpochMilliseconds(3),
            title = null,
            content = "child",
            images = listOf(Image("https://example.test/full.png", "https://example.test/thumb.png")),
            isAdmin = false,
            floor = 2,
            replyToId = "parent",
            sourceId = "tieba",
        )
        val parent = Comment(
            id = "parent",
            topicId = "topic",
            author = author,
            createdAt = Instant.fromEpochMilliseconds(2),
            title = null,
            content = "parent",
            isAdmin = false,
            floor = 2,
            sourceId = "tieba",
            subCommentCount = 1,
            subCommentsPreview = listOf(child),
        )
        cache.saveComments(listOf(parent), "tieba", "topic", "all", 1, 100, 0)

        val restored = database.commentQueries.getCommentById("tieba", "parent")
            .executeAsOne()
            .toDomain(database.imageQueries, database.commentQueries)
        assertEquals("child", restored.subCommentsPreview.single().id)
        assertEquals("https://example.test/full.png", restored.subCommentsPreview.single().images.single().originalUrl)
        driver.close()
    }

    @Test
    fun imageReplacementAndParentChildGraphSurviveDesktopRestart() = runBlocking {
        val file = Files.createTempFile("thread-forum-offline-", ".db")
        val url = "jdbc:sqlite:$file"
        try {
            JdbcSqliteDriver(url).use { driver ->
                Database.Schema.synchronous().create(driver)
                val database = createDatabase(driver)
                val cache = SqlDelightSourceCache(database)
                val author = Author("author", "Author", sourceName = "Tieba")
                cache.saveTopic(topic(author))

                val oldChild = child(author, "https://example.test/old.png")
                cache.saveComments(
                    comments = listOf(parent(author, oldChild)),
                    sourceId = "tieba",
                    topicId = "topic",
                    viewMode = "all",
                    page = 1,
                    receiveDate = 100,
                    startOrder = 0,
                )
                val replacedChild = child(author, "https://example.test/new.png")
                cache.saveComments(
                    comments = listOf(parent(author, replacedChild)),
                    sourceId = "tieba",
                    topicId = "topic",
                    viewMode = "all",
                    page = 1,
                    receiveDate = 200,
                    startOrder = 0,
                )
                assertEquals(
                    listOf("https://example.test/new.png"),
                    database.imageQueries.getImagesByParent("tieba", "child", ai.saniou.thread.domain.model.forum.ImageType.Comment)
                        .executeAsList().map { it.originalUrl },
                )
            }

            JdbcSqliteDriver(url).use { driver ->
                val database = createDatabase(driver)
                val restored = database.commentQueries.getCommentById("tieba", "parent")
                    .executeAsOne()
                    .toDomain(database.imageQueries, database.commentQueries)
                assertEquals("child", restored.subCommentsPreview.single().id)
                assertEquals(
                    "https://example.test/new.png",
                    restored.subCommentsPreview.single().images.single().originalUrl,
                )
                assertEquals("Topic", database.topicQueries.getTopic("tieba", "topic").executeAsOne().title)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun topic(author: Author) = Topic(
        id = "topic",
        channelId = "one",
        channelName = "One",
        title = "Topic",
        content = "body",
        summary = "summary",
        author = author,
        createdAt = Instant.fromEpochMilliseconds(1),
        commentCount = 1,
        sourceId = "tieba",
        sourceName = "Tieba",
        sourceUrl = "https://example.test/topic",
    )

    private fun child(author: Author, imageUrl: String) = Comment(
        id = "child",
        topicId = "topic",
        author = author,
        createdAt = Instant.fromEpochMilliseconds(3),
        title = null,
        content = "child",
        images = listOf(Image(imageUrl, imageUrl)),
        isAdmin = false,
        floor = 2,
        replyToId = "parent",
        sourceId = "tieba",
    )

    private fun parent(author: Author, child: Comment) = Comment(
        id = "parent",
        topicId = "topic",
        author = author,
        createdAt = Instant.fromEpochMilliseconds(2),
        title = null,
        content = "parent",
        isAdmin = false,
        floor = 2,
        sourceId = "tieba",
        subCommentCount = 1,
        subCommentsPreview = listOf(child),
    )

    private fun channel(id: String) = Channel(
        id = id,
        name = id,
        displayName = id,
        description = "",
        descriptionText = null,
        groupId = "group",
        groupName = "Group",
        sourceName = "Tieba",
        sourceId = "tieba",
        sort = 0,
        topicCount = null,
        postCount = null,
        autoDelete = null,
    )
}
