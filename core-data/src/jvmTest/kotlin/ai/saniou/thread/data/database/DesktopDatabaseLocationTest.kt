package ai.saniou.thread.data.database

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopDatabaseLocationTest {
    @Test
    fun migratesLegacyDatabaseAndSidecarsOnlyWhenTargetIsAbsent() {
        val root = Files.createTempDirectory("thread-database-location-")
        try {
            val legacy = root.resolve("legacy/nmb.db")
            val target = root.resolve("home/.thread/nmb.db")
            Files.createDirectories(legacy.parent)
            legacy.writeText("legacy-database")
            legacy.resolveSibling("nmb.db-wal").writeText("legacy-wal")
            legacy.resolveSibling("nmb.db-shm").writeText("legacy-shm")

            migrateLegacyDesktopDatabase(legacy.toFile(), target.toFile())

            assertEquals("legacy-database", target.readText())
            assertEquals("legacy-wal", target.resolveSibling("nmb.db-wal").readText())
            assertEquals("legacy-shm", target.resolveSibling("nmb.db-shm").readText())

            legacy.writeText("new-legacy-value")
            migrateLegacyDesktopDatabase(legacy.toFile(), target.toFile())
            assertEquals("legacy-database", target.readText(), "an existing product database must never be overwritten")
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun ignoresMissingLegacyDatabase() {
        val root = Files.createTempDirectory("thread-database-location-empty-")
        try {
            val target = root.resolve("home/.thread/nmb.db")
            migrateLegacyDesktopDatabase(root.resolve("missing.db").toFile(), target.toFile())
            assertFalse(Files.exists(target))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
