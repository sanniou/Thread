package ai.saniou.nmb.data.database

import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.FavoriteForum
import ai.saniou.nmb.db.table.RemoteKeys
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    val database = Database(
        driver = driver,
        FavoriteForumAdapter = FavoriteForum.Adapter(EnumColumnAdapter()),
        RemoteKeysAdapter = RemoteKeys.Adapter(EnumColumnAdapter()),
    )
    return database
}

internal val DATABADE_FILE_NAME = "nmb.db"
