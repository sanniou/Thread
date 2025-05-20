package ai.saniou.nmb.data.database

import ai.saniou.nmb.data.db.Database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, DATABADE_FILE_NAME)
    }
}

bind<DriverFactory>() with singleton { AndroidDriverFactory(androidContext()) }
