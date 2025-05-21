package ai.saniou.nmb.data.database

import ai.saniou.nmb.db.Database
import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    val database = Database(driver)
    return database
}

internal val DATABADE_FILE_NAME = "nmb.db"
