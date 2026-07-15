package ai.saniou.thread.data.cache

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.cache.CachePolicy
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class CacheFreshnessStoreTest {
    @Test
    fun tracksAndInvalidatesResourceFreshness() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val store = CacheFreshnessStore(createDatabase(driver))
        val key = CacheFreshnessStore.topic("source", "42")
        val policy = CachePolicy(5.minutes)

        assertFalse(store.isFresh(key, policy))
        store.markFresh(key)
        assertTrue(store.isFresh(key, policy))
        assertFalse(store.isFresh(key, CachePolicy(Duration.ZERO)))
        store.invalidate(key)
        assertFalse(store.isFresh(key, policy))
        driver.close()
    }
}
