package ai.saniou.thread.data.repository

import ai.saniou.corecommon.utils.UuidUtils
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.paging.KeysetPagingSource
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Trend
import ai.saniou.thread.db.table.TrendDetailView
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.source.TrendSource
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class TrendRepositoryImpl(
    private val sources: Set<TrendSource>, // Injected by DI
    private val db: Database
) : TrendRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getAvailableTrendSources(): List<TrendSource> {
        return sources.toList()
    }

    override fun getTrendSource(id: String): TrendSource? {
        return sourceMap[id]
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getTrendPagingData(sourceId: String, tab: TrendTab, params: TrendParams): Flow<PagingData<TrendItem>> {
        val source = getTrendSource(sourceId) ?: return emptyFlow()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val targetDate = today.minus(params.dayOffset, DateTimeUnit.DAY).toString()

        // Determine mode: Rank (Daily) or Realtime
        // If dayOffset > 0, it's definitely history (Rank).
        // If supportsHistory is true, it's likely Rank based.
        // Otherwise, assume Realtime.
        val isRankMode = tab.supportsHistory || params.dayOffset > 0

        val remoteKeyId = "${source.id}_${tab.id}_$targetDate"

        val mediator = GenericRemoteMediator<Long, TrendDetailView, TrendItem>(
            db = db,
            dataPolicy = if (params.refreshId > 0) DataPolicy.NETWORK_ONLY else DataPolicy.CACHE_ELSE_NETWORK,
            initialKey = if (isRankMode) 1L else Long.MAX_VALUE,
            remoteKeyStrategy = DefaultRemoteKeyStrategy(
                db = db,
                type = RemoteKeyType.TREND,
                id = remoteKeyId,
                serializer = { it.toString() },
                deserializer = { it.toLong() }
            ),
            fetcher = { key ->
                // Convert Long Key to String cursor for Source
                source.fetchTrendData(tab, params, key.toString())
            },
            saver = { items, key, loadType ->
                if (loadType == LoadType.REFRESH) {
                    if (isRankMode) {
                        db.trendQueries.deleteTrendsByTabAndDate(source.id, tab.id, targetDate)
                    } else {
                        // For realtime, we might not want to delete everything on refresh,
                        // but to keep it simple and consistent with "pull to refresh", we can.
                        // Or we can rely on upsert to update existing items.
                        // Let's keep it simple: delete only if we are sure we got a fresh batch that replaces old one.
                        // But for infinite stream, refresh usually means "get latest".
                        // We don't delete old data in realtime mode to support offline browsing of history.
                    }
                }

                items.forEachIndexed { index, item ->
                    db.trendQueries.upsert(
                        Trend(
                            id = UuidUtils.randomUuid(), // Use UUID to support multiple occurrences
                            sourceId = source.id,
                            tabId = tab.id,
                            topicId = item.topicId,
                            date = targetDate,
                            rank = item.rank?.toLong() ?: (index + 1).toLong(),
                            page = 0L, // Not used in Keyset
                            title = item.title,
                            contentPreview = item.contentPreview,
                            hotness = item.hotness,
                            channel = item.channel,
                            author = item.author,
                            url = item.url,
                            isNew = item.isNew,
                            payload = null, // TODO: Serialize payload if needed
                            publishDate = null, // Source doesn't provide it yet
                            updateDate = null,
                            receiveDate = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            },
            itemsExtractor = { it },
            endOfPaginationReached = { items ->
                source.trendDataEnded(tab, params, items)
            },
            cacheChecker = { key ->
                if (isRankMode) {
                    // Check if we have data for this specific date
                    val count = db.trendQueries.countTrends(source.id, tab.id, targetDate).executeAsOne()
                    count > 0
                } else {
                    // For realtime, check if we have data older than key
                    // This is a bit tricky. If key is MAX_VALUE (Refresh), we always want network (handled by DataPolicy).
                    // If key is a timestamp (Append), we check if we have older data.
                    // But GenericRemoteMediator logic for CACHE_ELSE_NETWORK is: if cacheChecker returns true, skip network.
                    // So we should return true if we have enough data.
                    // For now, let's assume if we have *any* data, we use it? No, that prevents loading more.
                    // We need to check if we have data *after* the current key.
                    // Since we don't have a query for that easily available without limit, let's return false to force network for Append
                    // unless we implement a specific count query.
                    false
                }
            },
            keyExtractor = { item ->
                if (isRankMode) item.rank?.toLong() ?: 0L else item.receiveDate
            }
        )

        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            remoteMediator = mediator,
            pagingSourceFactory = {
                if (isRankMode) {
                    KeysetPagingSource(
                        transacter = db.trendQueries,
                        context = Dispatchers.IO,
                        queryProvider = { key, limit ->
                            db.trendQueries.getTrendsKeyset(
                                sourceId = source.id,
                                tabId = tab.id,
                                date = targetDate,
                                lastRank = key?: 0,
                                limit = limit
                            )
                        },
                        countQueryProvider = {
                            db.trendQueries.countTrends(source.id, tab.id, targetDate)
                        },
                        keyExtractor = { trend -> trend.rank.toLong() }
                    )
                } else {
                    KeysetPagingSource(
                        transacter = db.trendQueries,
                        context = Dispatchers.IO,
                        queryProvider = { key, limit ->
                            db.trendQueries.getRealtimeTrendsKeyset(
                                sourceId = source.id,
                                tabId = tab.id,
                                lastReceiveDate = key ?: Long.MAX_VALUE,
                                limit = limit
                            )
                        },
                        countQueryProvider = {
                            db.trendQueries.countTrends(source.id, tab.id, targetDate) // This count might be inaccurate for realtime but ok for invalidation
                        },
                        keyExtractor = { trend -> trend.receiveDate }
                    )
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { trend ->
                TrendItem(
                    topicId = trend.topicId,
                    sourceId = trend.sourceId,
                    title = trend.title,
                    contentPreview = trend.contentPreview,
                    rank = trend.rank.toInt(),
                    hotness = trend.hotness,
                    channel = trend.channel,
                    author = trend.author,
                    url = trend.url,
                    isNew = trend.isNew,
                    payload = emptyMap(),
                    receiveDate = trend.receiveDate
                )
            }
        }
    }
}
