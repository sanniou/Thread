package ai.saniou.thread.data.database

import ai.saniou.thread.data.platform.AndroidPlatformContext
import ai.saniou.thread.db.Database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = Database.Schema,
            context = AndroidPlatformContext.requireContext(),
            name = DATABADE_FILE_NAME,
        )
    }
}
