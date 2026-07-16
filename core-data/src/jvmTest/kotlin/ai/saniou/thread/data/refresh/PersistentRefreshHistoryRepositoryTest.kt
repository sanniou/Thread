package ai.saniou.thread.data.refresh

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.data.repository.SettingsRepositoryImpl
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import ai.saniou.thread.domain.refresh.RefreshPolicy
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PersistentRefreshHistoryRepositoryTest {
    @Test
    fun failureHistorySurvivesRepositoryRestartAndSuccessRecoversIt() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val settings = SettingsRepositoryImpl(createDatabase(driver))
        val history = PersistentRefreshHistoryRepository(settings)
        val coordinator = DefaultRefreshCoordinator(history)

        coordinator.execute<Unit>(
            key = "reader:secure-feed",
            label = "Secure Feed",
            policy = RefreshPolicy(maxAttempts = 1),
        ) {
            Result.failure(IllegalStateException(
                "HTTP 429 token=raw-token cookie=raw-cookie https://example.test/feed?secret=raw-query"
            ))
        }

        val persisted = PersistentRefreshHistoryRepository(settings).observe().first()
            .getValue("reader:secure-feed")
        assertEquals(RefreshFailureKind.RATE_LIMIT, persisted.failureKind)
        assertEquals(1, persisted.consecutiveFailureCount)
        assertNotNull(persisted.rateLimitUntilEpochMillis)
        assertTrue("[REDACTED]" in persisted.message.orEmpty())
        listOf("raw-token", "raw-cookie", "raw-query").forEach { secret ->
            assertFalse(secret in persisted.message.orEmpty())
        }

        DefaultRefreshCoordinator(PersistentRefreshHistoryRepository(settings)).execute(
            key = "reader:secure-feed",
            label = "Secure Feed",
            policy = RefreshPolicy(maxAttempts = 1),
        ) { Result.success(Unit) }

        val recovered = PersistentRefreshHistoryRepository(settings).observe().first()
            .getValue("reader:secure-feed")
        assertEquals(0, recovered.consecutiveFailureCount)
        assertEquals(null, recovered.failureKind)
        assertEquals(null, recovered.rateLimitUntilEpochMillis)
        assertNotNull(recovered.lastSuccessAtEpochMillis)
        driver.close()
    }
}
