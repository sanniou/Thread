package ai.saniou.thread.data.database

import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Bookmark
import ai.saniou.thread.db.table.FavoriteForum
import ai.saniou.thread.db.table.RemoteKeys
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import kotlin.time.Instant

expect class DriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    val database = Database(
        driver = driver,
        FavoriteForumAdapter = FavoriteForum.Adapter(EnumColumnAdapter()),
        RemoteKeysAdapter = RemoteKeys.Adapter(EnumColumnAdapter()),
        BookmarkAdapter = Bookmark.Adapter(object : ColumnAdapter<Instant, Long> {
            override fun decode(databaseValue: Long): Instant {
                return Instant.fromEpochMilliseconds(databaseValue)
            }

            override fun encode(value: Instant): Long {
                return value.toEpochMilliseconds()
            }
        }),
    )
    return database
}

internal val DATABADE_FILE_NAME = "nmb.db"
