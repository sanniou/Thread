package ai.saniou.nmb.data.database

import ai.saniou.nmb.db.Database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, DATABADE_FILE_NAME)
    }
}
