package ai.saniou.thread

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.repository.SyncRepository
import ai.saniou.thread.domain.source.SourceCatalog
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppCompositionSmokeTest {
    @Test
    fun desktopCompositionResolvesConformantRuntimeCatalog() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val di = createAppDi(createDatabase(driver))

        val catalog = di.direct.instance<SourceCatalog>()
        val descriptors = catalog.descriptors.value

        assertEquals(setOf("nmb", "tieba", "discourse"), descriptors.mapTo(mutableSetOf()) { it.id })
        val nmb = catalog.source("nmb")!!
        assertTrue(nmb.capabilities.supportsAttachments)
        assertNotNull(catalog.search("nmb"))
        assertNotNull(catalog.userContent("nmb"))
        assertNotNull(catalog.posting("nmb"))
        assertNotNull(catalog.login("nmb"))

        val tieba = catalog.source("tieba")!!
        assertTrue(tieba.capabilities.hasSubComments)
        assertTrue(tieba.capabilities.hasUpvote)
        assertNull(catalog.search("tieba"))
        assertNotNull(catalog.userContent("tieba"))
        assertNotNull(catalog.posting("tieba"))
        assertNotNull(catalog.login("tieba"))
        assertNotNull(catalog.subComments("tieba"))
        assertNotNull(catalog.reactions("tieba"))

        val discourse = catalog.source("discourse")!!
        assertTrue(discourse.capabilities.supportsTopicCreation)
        assertNotNull(catalog.search("discourse"))
        assertNotNull(catalog.userContent("discourse"))
        assertNotNull(catalog.posting("discourse"))
        assertNotNull(catalog.login("discourse"))
        assertNull(catalog.subComments("discourse"))
        assertNull(catalog.reactions("discourse"))
        assertNotNull(di.direct.instance<SyncRepository>())

        di.direct.instance<ReaderRefreshScheduler>().stop()
    }
}
