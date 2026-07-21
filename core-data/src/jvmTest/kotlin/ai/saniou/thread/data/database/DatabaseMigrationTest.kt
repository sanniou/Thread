package ai.saniou.thread.data.database

import ai.saniou.thread.db.Database
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseMigrationTest {
    @Test
    fun migratesVersionOneReaderStateWithoutRecreatingDatabase() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        Database.Schema.synchronous().migrate(driver, oldVersion = 1L, newVersion = 3L)
        val database = createDatabase(driver)
        database.articleQueries.upsertArticleUserState(
            articleId = "migrated",
            isRead = 1L,
            isBookmarked = 1L,
            updatedAt = 42L,
        )

        val state = database.articleQueries.getArticleUserState("migrated").executeAsOne()
        assertEquals(1L, state.isRead)
        assertEquals(1L, state.isBookmarked)
        assertEquals(42L, state.updatedAt)
        database.channelQueries.upsertChannelVisit("nmb", "4", 99L)
        assertEquals(99L, database.channelQueries.getChannelVisit("nmb", "4").executeAsOne().visitedAt)
        driver.close()
    }

    @Test
    fun addsVersionFourPagingIndexesWithoutRecreatingCachedContent() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.executeStatement(
            """
            CREATE TABLE ArticleEntity (
                feedSourceId TEXT NOT NULL,
                publishDate INTEGER NOT NULL,
                isRead INTEGER NOT NULL,
                isBookmarked INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        driver.executeStatement(
            """
            CREATE TABLE Subscription (
                subscriptionKey TEXT NOT NULL,
                subscriptionTime INTEGER NOT NULL,
                topicId TEXT NOT NULL
            )
            """.trimIndent(),
        )
        driver.executeStatement(
            """
            CREATE TABLE Comment (
                sourceId TEXT NOT NULL,
                topicId TEXT NOT NULL,
                floor INTEGER NOT NULL,
                userHash TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        driver.executeStatement(
            """
            CREATE TABLE Topic (
                sourceId TEXT NOT NULL,
                channelId TEXT NOT NULL,
                authorId TEXT NOT NULL,
                lastReplyAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        Database.Schema.synchronous().migrate(driver, oldVersion = 3L, newVersion = 4L)

        val expected = setOf(
            "idx_article_source_published",
            "idx_article_unread_published",
            "idx_article_bookmarked_published",
            "idx_subscription_key_time",
            "idx_comment_topic_floor",
            "idx_comment_source_user_created",
            "idx_topic_source_channel_reply",
            "idx_topic_source_author_reply",
        )
        assertTrue(driver.indexNames().containsAll(expected))
        assertEquals(6L, Database.Schema.version)
        driver.close()
    }

    @Test
    fun addsVersionFiveInboxWithoutTouchingExistingTables() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().migrate(driver, oldVersion = 4L, newVersion = 5L)
        driver.executeStatement(
            """
            INSERT INTO InboxEventEntity(
                id, kind, sourceId, title, summary, occurredAt, muted, priority
            ) VALUES ('migration', 'SYSTEM', 'thread', 'Ready', '', 1, 0, 0)
            """.trimIndent(),
        )
        val count = driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM InboxEventEntity",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            parameters = 0,
        ).value
        assertEquals(1L, count)
        driver.close()
    }

    @Test
    fun addsVersionSixSocialGraphAndCollectionIndex() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().migrate(driver, oldVersion = 4L, newVersion = 6L)
        driver.executeStatement(
            """
            INSERT INTO SocialSourceEntity(id, displayName, baseUrl, enabled, lastSyncAt)
            VALUES ('mastodon', 'Mastodon', 'https://example.social', 1, NULL)
            """.trimIndent(),
        )
        driver.executeStatement(
            """
            INSERT INTO SocialPostEntity(
                id, sourceId, authorId, authorName, body, createdAt
            ) VALUES ('1', 'mastodon', 'u1', 'Alice', 'hello graph', 10)
            """.trimIndent(),
        )
        driver.executeStatement(
            """
            INSERT INTO ContentGraphEdgeEntity(
                fromKind, fromId, fromSourceId, fromParentId,
                toKind, toId, toSourceId, toParentId,
                relation, weight, createdAt
            ) VALUES (
                'SOCIAL_POST', '1', 'mastodon', '',
                'SOCIAL_POST', '0', 'mastodon', '',
                'REPLY_TO', 1.0, 10
            )
            """.trimIndent(),
        )
        driver.executeStatement(
            """
            INSERT INTO SmartCollectionIndexEntity(
                contentKey, contentKind, contentId, sourceId, sourceName,
                title, body, author, publishedAt
            ) VALUES (
                'SOCIAL_POST:mastodon:1', 'SOCIAL_POST', '1', 'mastodon', 'Mastodon',
                'Alice', 'hello graph', 'Alice', 10
            )
            """.trimIndent(),
        )
        val socialCount = driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM SocialPostEntity",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            parameters = 0,
        ).value
        val edgeCount = driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM ContentGraphEdgeEntity",
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            parameters = 0,
        ).value
        assertEquals(1L, socialCount)
        assertEquals(1L, edgeCount)
        assertEquals(6L, Database.Schema.version)
        driver.close()
    }

    private fun SqlDriver.executeStatement(sql: String) {
        execute(identifier = null, sql = sql, parameters = 0).value
    }

    private fun SqlDriver.indexNames(): Set<String> = executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'index'",
        mapper = { cursor ->
            val names = buildSet {
                while (cursor.next().value) {
                    cursor.getString(0)?.let(::add)
                }
            }
            QueryResult.Value(names)
        },
        parameters = 0,
    ).value
}
