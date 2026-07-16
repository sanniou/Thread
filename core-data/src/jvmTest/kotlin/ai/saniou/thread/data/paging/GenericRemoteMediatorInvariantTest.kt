package ai.saniou.thread.data.paging

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.PagedResult
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GenericRemoteMediatorInvariantTest {
    @Test
    fun failedRefreshKeepsCacheAndSuccessfulRefreshCommitsRowsAndKeysTogether() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        val keys = RecordingRemoteKeys()
        val cached = mutableListOf("cached-a", "cached-b")
        var attempts = 0
        val mediator = GenericRemoteMediator<String, String>(
            db = database,
            dataPolicy = DataPolicy.NETWORK_ONLY,
            remoteKeyStrategy = keys,
            fetcher = {
                attempts += 1
                if (attempts == 1) Result.failure(IllegalStateException("HTTP 503"))
                else Result.success(PagedResult(listOf("fresh-a", "fresh-b"), null, null))
            },
            lastItemMetadataExtractor = { null },
            saver = { items, loadType, _, _, _ ->
                if (loadType == LoadType.REFRESH) cached.clear()
                cached += items
            },
            itemTargetIdExtractor = { it },
        )
        val state = PagingState<Int, String>(
            pages = listOf(PagingSource.LoadResult.Page(cached.toList(), null, null)),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0,
        )

        assertIs<RemoteMediator.MediatorResult.Error>(mediator.load(LoadType.REFRESH, state))
        assertEquals(listOf("cached-a", "cached-b"), cached)
        assertEquals(0, keys.clearCount)
        assertEquals(emptyList(), keys.inserted)

        val success = assertIs<RemoteMediator.MediatorResult.Success>(
            mediator.load(LoadType.REFRESH, state)
        )
        assertEquals(listOf("fresh-a", "fresh-b"), cached)
        assertEquals(1, keys.clearCount)
        assertEquals(listOf("fresh-a", "fresh-b"), keys.inserted)
        assertEquals(true, success.endOfPaginationReached)
        driver.close()
    }
}

private class RecordingRemoteKeys : RemoteKeyStrategy<String> {
    var clearCount = 0
    val inserted = mutableListOf<String>()

    override suspend fun getKeyForFirstItem(state: PagingState<Int, String>): String? = null
    override suspend fun getKeyForLastItem(state: PagingState<Int, String>): String? = null
    override suspend fun getKeyClosestToCurrentPosition(state: PagingState<Int, String>): String? = null
    override suspend fun insertKeys(targetId: String, prevKey: String?, nextKey: String?) {
        inserted += targetId
    }
    override suspend fun clearKeys() {
        clearCount += 1
    }
}
