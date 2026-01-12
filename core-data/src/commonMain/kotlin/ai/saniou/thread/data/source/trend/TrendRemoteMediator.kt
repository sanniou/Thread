package ai.saniou.thread.data.source.trend

import ai.saniou.corecommon.utils.UuidUtils
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.paging.RemoteKeyStrategy
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.GetTrendsWithTopic
import ai.saniou.thread.db.table.Trend
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.LoadType
import app.cash.paging.RemoteMediator
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalPagingApi::class)
class TrendRemoteMediator(
    private val db: Database,
    private val source: TrendSource,
    private val tab: TrendTab,
    private val params: TrendParams,
) {
    // Generate a unique key for RemoteKeys based on source, tab, and date
    // This ensures pagination state is tracked separately for each day
    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val targetDate = today.minus(params.dayOffset, DateTimeUnit.DAY).toString()

    private val remoteKeyId = "${source.id}_${tab.id}_$targetDate"

    val mediator: RemoteMediator<Int, GetTrendsWithTopic> = GenericRemoteMediator(
        db = db,
        dataPolicy = if (params.forceRefresh) DataPolicy.NETWORK_ONLY else DataPolicy.CACHE_ELSE_NETWORK,
        initialKey = 1,
        remoteKeyStrategy = object : RemoteKeyStrategy<Int, GetTrendsWithTopic> {
            override suspend fun getKeyClosestToCurrentPosition(state: app.cash.paging.PagingState<Int, GetTrendsWithTopic>): ai.saniou.thread.db.table.RemoteKeys? {
                return db.remoteKeyQueries.getRemoteKeyById(RemoteKeyType.TREND, remoteKeyId)
                    .executeAsOneOrNull()
            }

            override suspend fun getKeyForFirstItem(state: app.cash.paging.PagingState<Int, GetTrendsWithTopic>): ai.saniou.thread.db.table.RemoteKeys? {
                return db.remoteKeyQueries.getRemoteKeyById(RemoteKeyType.TREND, remoteKeyId)
                    .executeAsOneOrNull()
            }

            override suspend fun getKeyForLastItem(state: app.cash.paging.PagingState<Int, GetTrendsWithTopic>): ai.saniou.thread.db.table.RemoteKeys? {
                return db.remoteKeyQueries.getRemoteKeyById(RemoteKeyType.TREND, remoteKeyId)
                    .executeAsOneOrNull()
            }

            override suspend fun insertKeys(
                key: Int,
                prevKey: Int?,
                nextKey: Int?,
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
                // Clear old data for this specific tab and date
                db.trendQueries.deleteTrendsByTabAndDate(source.id, tab.id, targetDate)
            }

            items.forEachIndexed { index, item ->
                // 1. Upsert Topic if possible (optional, but good for data consistency)
                // We don't have full topic details here usually, but if we did, we'd save it.
                // For now, we rely on Trend table holding the snapshot.

                // 2. Insert Trend
                db.trendQueries.upsert(
                    Trend(
                        id = UuidUtils.randomUuid(), // Use UUID as primary key
                        sourceId = source.id,
                        tabId = tab.id,
                        topicId = item.id, // Link to Topic ID
                        date = targetDate,
                        rank = item.rank?.toLong() ?: (index + 1).toLong(),
                        page = page.toLong(),
                        title = item.title,
                        contentPreview = item.contentPreview,
                        hotness = item.hotness,
                        channel = item.channel,
                        author = item.author,
                        url = item.url,
                        isNew = item.isNew,
                        payload = null, // TODO: Serialize payload if needed
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        },
        endOfPaginationReached = { items ->
            items.isEmpty()
        },
        cacheChecker = { page ->
            // Check if we have data for this specific date
            val count = db.trendQueries.countTrends(source.id, tab.id, targetDate).executeAsOne()
            count > 0
        },
        keyIncrementer = { it + 1 },
        keyDecrementer = { it - 1 },
        keyToLong = { it.toLong() },
        longToKey = { it.toInt() }
    )
}
