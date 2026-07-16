package ai.saniou.thread.data.database

import ai.saniou.thread.db.Database
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopDatabaseRestartTest {
    @Test
    fun settingsAndReaderStateSurviveARealDatabaseRestart() = runBlocking {
        val databaseFile = Files.createTempFile("thread-desktop-restart-", ".db")
        val jdbcUrl = "jdbc:sqlite:$databaseFile"

        try {
            JdbcSqliteDriver(jdbcUrl).use { driver ->
                Database.Schema.synchronous().create(driver)
                val database = createDatabase(driver)
                database.keyValueQueries.insertKeyValue("current_source_id", "tieba")
                database.articleQueries.upsertArticleUserState(
                    articleId = "offline-article",
                    isRead = 1L,
                    isBookmarked = 1L,
                    updatedAt = 42L,
                )
            }

            JdbcSqliteDriver(jdbcUrl).use { driver ->
                val database = createDatabase(driver)
                assertEquals(
                    "tieba",
                    database.keyValueQueries.getKeyValue("current_source_id")
                        .executeAsOne().content,
                )
                val state = database.articleQueries.getArticleUserState("offline-article")
                    .executeAsOne()
                assertEquals(1L, state.isRead)
                assertEquals(1L, state.isBookmarked)
                assertEquals(42L, state.updatedAt)
            }
        } finally {
            Files.deleteIfExists(databaseFile)
        }
    }
}
