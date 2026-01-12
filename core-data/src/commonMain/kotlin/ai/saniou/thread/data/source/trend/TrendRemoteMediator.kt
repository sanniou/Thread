package ai.saniou.thread.data.source.trend

import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.paging.RemoteKeyStrategy
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Trend
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.RemoteMediator
import kotlin.time.Clock

@OptIn(ExperimentalPagingApi::class)
class TrendRemoteMediator(
    private val db: Database,
    private val source: TrendSource,
    private val tab: TrendTab,
    private val params: TrendParams,
) {
    private val remoteKeyId = "${source.id}_${tab.id}"

    val mediator: RemoteMediator<Int, Trend> = GenericRemoteMediator(
        db = db,
        dataPolicy = if (params.forceRefresh) DataPolicy.NETWORK_ONLY else DataPolicy.CACHE_ELSE_NETWORK,
        initialKey = 1,
        remoteKeyStrategy = object : RemoteKeyStrategy<Int, Trend> {
            override suspend fun getKeyClosestToCurrentPosition(state: app.cash.paging.PagingState<Int, Trend>): ai.saniou.thread.db.table.RemoteKeys? {
                return db.remoteKeyQueries.getRemoteKeyById(RemoteKeyType.TREND, remoteKeyId)
                    .executeAsOneOrNull()
            }

            override suspend fun getKeyForFirstItem(state: app.cash.paging.PagingState<Int, Trend>): ai.saniou.thread.db.table.RemoteKeys? {
                return db.remoteKeyQueries.getRemoteKeyById(RemoteKeyType.TREND, remoteKeyId)
                    .executeAsOneOrNull()
            }

            override suspend fun getKeyForLastItem(state: app.cash.paging.PagingState<Int, Trend>): ai.saniou.thread.db.table.RemoteKeys? {
                return db.remoteKeyQueries.getRemoteKeyById(RemoteKeyType.TREND, remoteKeyId)
                    .executeAsOneOrNull()
            }

            /**
             * Override function to insert pagination keys into the database
             * @param key The current page key to be inserted
             * @param prevKey The previous page key, can be null if it's the first page
             * @param nextKey The next page key, can be null if it's the last page
             * @param endOfPaginationReached Flag indicating if the end of pagination has been reached
             */
            override suspend fun insertKeys(
                key: Int,                    // Current page number as integer
                prevKey: Int?,              // Previous page number, nullable
                nextKey: Int?,              // Next page number, nullable
                endOfPaginationReached: Boolean,
            ) {
                db.remoteKeyQueries.insertKey(
                    type = RemoteKeyType.TREND,
                    id = remoteKeyId,
                    prevKey = prevKey?.toLong(),
                    currKey = key.toLong(),
                    nextKey = nextKey?.toLong(),
                    updateAt = Clock.System.now().toEpochMilliseconds()
                )
            }
        },
        fetcher = { page ->
            source.fetchTrendData(tab, params, page)
        },
        saver = { items, page, loadType ->
            if (loadType == LoadType.REFRESH) {
                db.trendQueries.deleteTrendsByTab(source.id, tab.id)
            }
            items.forEachIndexed { index, item ->
                db.trendQueries.upsert(
                    Trend(
                        id = item.id,
                        sourceId = source.id,
                        tabId = tab.id,
                        title = item.title,
                        contentPreview = item.contentPreview,
                        rank = item.rank?.toLong() ?: (index + 1).toLong(),
                        hotness = item.hotness,
                        channel = item.channel,
                        author = item.author,
                        url = item.url,
                        isNew = item.isNew,
                        payload = null, // TODO: Serialize payload if needed
                        page = page.toLong(),
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        },
        endOfPaginationReached = { items ->
            items.isEmpty()
        },
        cacheChecker = { page ->
            // Simple cache check: if we have data for this page, use it.
            // More complex logic (e.g. TTL) can be added here.
            val count = db.trendQueries.countTrends(source.id, tab.id).executeAsOne()
            count > 0
        },
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() },
        longToKey = { it.toInt() }
    )
}
