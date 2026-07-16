package ai.saniou.thread.data.database

import ai.saniou.thread.db.Database
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationTest {
    @Test
    fun migratesVersionOneReaderStateWithoutRecreatingDatabase() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        Database.Schema.synchronous().migrate(driver, oldVersion = 1L, newVersion = 2L)
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
        driver.close()
    }
}
