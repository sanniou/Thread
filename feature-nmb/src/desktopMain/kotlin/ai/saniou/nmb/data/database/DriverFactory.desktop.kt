package ai.saniou.nmb.data.database

import ai.saniou.nmb.db.Database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver =
            JdbcSqliteDriver("jdbc:sqlite:${DATABADE_FILE_NAME}", Properties(), Database.Schema)
        return driver
    }
}

