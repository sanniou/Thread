package ai.saniou.nmb.data.database

import ai.saniou.nmb.data.entity.FavoriteForumType
import ai.saniou.nmb.data.entity.RemoteKeyType
import ai.saniou.nmb.db.Database
import ai.saniou.nmb.db.table.FavoriteForum
import ai.saniou.nmb.db.table.RemoteKeys
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    val database = Database(
        driver = driver,
        FavoriteForumAdapter = FavoriteForum.Adapter(
            object :
                ColumnAdapter<FavoriteForumType, String> {
                override fun decode(databaseValue: String) =
                    FavoriteForumType.valueOf(databaseValue)

                override fun encode(value: FavoriteForumType) = value.name
            }),

        RemoteKeysAdapter = RemoteKeys.Adapter(object :
            ColumnAdapter<RemoteKeyType, String> {
            override fun decode(databaseValue: String) =
                RemoteKeyType.valueOf(databaseValue)

            override fun encode(value: RemoteKeyType) = value.name
        }),
    )
    return database
}

internal val DATABADE_FILE_NAME = "nmb.db"
