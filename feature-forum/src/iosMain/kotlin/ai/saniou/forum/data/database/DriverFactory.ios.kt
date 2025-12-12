package ai.saniou.forum.data.database

import ai.saniou.forum.data.db.Database
import ai.saniou.thread.data.database.DATABADE_FILE_NAME
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, DATABADE_FILE_NAME)
    }
}
