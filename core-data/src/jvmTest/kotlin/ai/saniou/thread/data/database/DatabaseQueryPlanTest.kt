package ai.saniou.thread.data.database

import ai.saniou.thread.db.Database
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/** Deterministic performance gate: query plans must keep using the release indexes. */
class DatabaseQueryPlanTest {
    @Test
    fun releaseListQueriesUseBoundedWindowIndexes() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)

        driver.assertUsesIndex(
            "idx_article_source_published",
            """
            SELECT * FROM ArticleEntity
            WHERE feedSourceId = 'reader'
            ORDER BY publishDate DESC LIMIT 20
            """.trimIndent(),
        )
        driver.assertUsesIndex(
            "idx_subscription_key_time",
            """
            SELECT * FROM Subscription
            WHERE subscriptionKey = 'following'
            ORDER BY subscriptionTime DESC, topicId DESC LIMIT 20
            """.trimIndent(),
        )
        driver.assertUsesIndex(
            "idx_comment_topic_floor",
            """
            SELECT * FROM Comment
            WHERE sourceId = 'forum' AND topicId = 'topic'
            ORDER BY floor ASC LIMIT 30
            """.trimIndent(),
        )
        driver.assertUsesIndex(
            "idx_topic_source_author_reply",
            """
            SELECT * FROM Topic
            WHERE sourceId = 'forum' AND authorId = 'author'
            ORDER BY lastReplyAt DESC LIMIT 20
            """.trimIndent(),
        )
        driver.close()
    }

    private fun SqlDriver.assertUsesIndex(index: String, query: String) {
        val plan = executeQuery(
            identifier = null,
            sql = "EXPLAIN QUERY PLAN $query",
            mapper = { cursor ->
                val details = buildList {
                    while (cursor.next().value) cursor.getString(3)?.let(::add)
                }
                QueryResult.Value(details)
            },
            parameters = 0,
        ).value
        assertTrue(
            plan.any { index in it },
            "Expected $index in query plan, got: ${plan.joinToString()}",
        )
    }
}
