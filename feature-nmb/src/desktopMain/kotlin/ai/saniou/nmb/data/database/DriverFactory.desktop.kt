package ai.saniou.nmb.data.database

import ai.saniou.nmb.db.Database
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        //val dbPath = "${System.getProperty("user.home")}/.myApp/mydb.db"

        //if (isDebug) {
        //  File(dbPath).delete() // 每次启动删掉旧文件，相当于 reset
        //}
        val driver: SqlDriver =
            JdbcSqliteDriver("jdbc:sqlite:${DATABADE_FILE_NAME}", Properties(), Database.Schema)
        return driver
    }
}

