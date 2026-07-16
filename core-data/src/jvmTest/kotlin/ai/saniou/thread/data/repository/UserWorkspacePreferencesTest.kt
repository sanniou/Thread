package ai.saniou.thread.data.repository

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.collection.SmartCollection
import ai.saniou.thread.domain.model.collection.SmartCollectionRules
import ai.saniou.thread.domain.model.settings.AppearancePreferences
import ai.saniou.thread.domain.model.settings.InterfaceDensity
import ai.saniou.thread.domain.model.settings.MotionMode
import ai.saniou.thread.domain.model.settings.ThemeMode
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class UserWorkspacePreferencesTest {
    @Test
    fun appearanceAndConcurrentCollectionsRoundTripWithBounds() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val settings = SettingsRepositoryImpl(database)
        val appearanceRepository = AppearanceRepositoryImpl(settings)
        val collectionRepository = SmartCollectionRepositoryImpl(settings, database)
        val appearance = AppearancePreferences(
            themeMode = ThemeMode.DARK,
            density = InterfaceDensity.COMPACT,
            fontScale = 1.2f,
            motionMode = MotionMode.REDUCED,
            readerLineHeight = 1.8f,
            readerWidthDp = 840,
        )
        appearanceRepository.save(appearance)
        assertEquals(appearance, appearanceRepository.observe().first())

        coroutineScope {
            (0 until 70).map { index ->
                async {
                    collectionRepository.save(
                        SmartCollection(
                            id = "collection-$index",
                            name = "Collection $index",
                            rules = SmartCollectionRules(query = "topic-$index"),
                            createdAtEpochMillis = index.toLong(),
                            updatedAtEpochMillis = index.toLong(),
                        )
                    )
                }
            }.awaitAll()
        }
        val collections = collectionRepository.observeCollections().first()
        assertEquals(50, collections.size)
        assertEquals(69L, collections.maxOf { it.updatedAtEpochMillis })
        database.feedSourceQueries.insertFeedSource(
            id = "reader", name = "Reader", url = "https://example.com/feed", type = "RSS",
            description = null, iconUrl = null, lastUpdate = 0, selectorConfig = "{}",
            autoRefresh = 1, refreshInterval = 3_600_000,
        )
        database.articleQueries.insertArticle(
            id = "article", feedSourceId = "reader", title = "topic-69 release",
            description = "matches the saved cross-source collection", content = "body",
            link = "https://example.com/article", author = "author", publishDate = 100,
            isRead = 0, isBookmarked = 0, imageUrl = null, rawContent = null,
        )
        val resolved = collectionRepository.resolve("collection-69")
        assertEquals(listOf("article"), resolved.map { it.id })
        driver.close()
    }
}
