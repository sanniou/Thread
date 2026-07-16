package ai.saniou.thread.data.database

import ai.saniou.thread.db.Database
import ai.saniou.thread.data.database.DATABADE_FILE_NAME
import ai.saniou.thread.data.storage.getStorageDirectory
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val storageDatabase = File(getStorageDirectory(), DATABADE_FILE_NAME)
        migrateLegacyDesktopDatabase(File(DATABADE_FILE_NAME).absoluteFile, storageDatabase)
        val driver: SqlDriver =
            JdbcSqliteDriver("jdbc:sqlite:${storageDatabase.absolutePath}", Properties(), Database.Schema.synchronous())
        return driver
    }

}

internal fun migrateLegacyDesktopDatabase(legacy: File, target: File) {
    if (target.exists() || !legacy.exists() || legacy.absoluteFile == target.absoluteFile) return
    target.parentFile?.mkdirs()
    legacy.copyTo(target, overwrite = false)
    listOf("-wal", "-shm").forEach { suffix ->
        val sidecar = File(legacy.absolutePath + suffix)
        if (sidecar.exists()) sidecar.copyTo(File(target.absolutePath + suffix), overwrite = false)
    }
}
