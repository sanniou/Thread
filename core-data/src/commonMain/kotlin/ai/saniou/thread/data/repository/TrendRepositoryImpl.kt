package ai.saniou.thread.data.repository

import ai.saniou.corecommon.utils.UuidUtils
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.paging.KeysetPagingSource
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Trend
import ai.saniou.thread.db.table.TrendDetailView
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.source.TrendSource
import androidx.paging.*
import app.cash.sqldelight.paging3.QueryPagingSource
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
    private val db: Database,
) : TrendRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    override fun getAvailableTrendSources(): List<TrendSource> {
        return sources.toList()
    }

    override fun getTrendSource(id: String): TrendSource? {
        return sourceMap[id]
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getTrendPagingData(
        sourceId: String,
        tab: TrendTab,
        params: TrendParams,
    ): Flow<PagingData<TrendItem>> {
        val source = getTrendSource(sourceId) ?: return emptyFlow()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val targetDate = today.minus(params.dayOffset, DateTimeUnit.DAY).toString()

        // Determine mode: Rank (Daily) or Realtime
        // If dayOffset > 0, it's definitely history (Rank).
        // If supportsHistory is true, it's likely Rank based.
        // Otherwise, assume Realtime.
        val isRankMode = tab.supportsHistory || params.dayOffset > 0

        val remoteKeyId = "${source.id}_${tab.id}_$targetDate"

        val mediator = GenericRemoteMediator<TrendDetailView, TrendItem>(
            db = db,
            dataPolicy = if (params.refreshId > 0) DataPolicy.NETWORK_ONLY else DataPolicy.CACHE_ELSE_NETWORK,
            remoteKeyStrategy = DefaultRemoteKeyStrategy(
                db = db,
                type = "trend_$remoteKeyId",
                itemTargetIdExtractor = { item -> item.topicId }
            ),
            fetcher = { cursor ->
                // Convert String cursor to Source fetch
                // Note: TrendSource.fetchTrendData returns Result<List<TrendItem>> currently.
                // We need to wrap it in PagedResult.
                // And we need to manually calculate next cursor if source doesn't provide it.
                source.fetchTrendData(tab, params, cursor).map { items ->
                    // Calculate next cursor
                    // If items is empty, next is null.
                    // If items is not empty, we need to know how to get next key.
                    // For Rank mode: next page = current page + 1? Or offset?
                    // For Realtime mode: next key = last item receiveDate.

                    val nextCursor = if (source.trendDataEnded(tab, params, items)) {
                        null
                    } else {
                        if (isRankMode) {
                            // Rank mode usually uses page or offset.
                            // Assuming cursor is page number string.
                            val page = cursor?.toIntOrNull() ?: 1
                            (page + 1).toString()
                        } else {
                            // Realtime mode uses timestamp
                            items.lastOrNull()?.receiveDate?.toString()
                        }
                    }

                    PagedResult(items, null, nextCursor)
                }
            },
            saver = { items, loadType, cursor, receiveDate, startOrder ->
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
                    // 1. Upsert Topic Content (if available)
                    // Note: TrendItem might not have full Topic details, but we save what we have.
                    // We use a dummy channelId if not provided, or try to infer.
                    // Ideally TrendItem should map to DomainTopic, but it's a simplified model.
                    // We save to Topic table to satisfy FK constraint and provide content for View.
                    db.topicQueries.upsertTopic(
                        Topic(
                            id = item.topicId,
                            sourceId = source.id,
                            channelId = "trend", // Placeholder, or use item.channel if it was an ID
                            commentCount = 0,
                            authorId = "",
                            authorName = item.author ?: "",
                            title = item.title,
                            content = null,
                            summary = item.contentPreview,
                            agreeCount = null,
                            disagreeCount = null,
                            isCollected = null,
                            createdAt = 0, // Unknown
                            lastReplyAt = 0,
                            lastVisitedAt = null,
                            lastViewedCommentId = null
                        )
                    )

                    // 2. Upsert Trend Metadata
                    db.trendQueries.upsert(
                        Trend(
                            id = UuidUtils.randomUuid(), // Use UUID to support multiple occurrences
                            sourceId = source.id,
                            tabId = tab.id,
                            topicId = item.topicId,
                            date = targetDate,
                            rank = item.rank?.toLong() ?: (index + 1).toLong(),
                            page = 0L, // Not used in Keyset
                            hotness = item.hotness,
                            isNew = item.isNew,
                            payload = null, // TODO: Serialize payload if needed
                            publishDate = null, // Source doesn't provide it yet
                            updateDate = null,
                            receiveDate = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            },
            itemTargetIdExtractor = { item -> item.topicId },
            cacheChecker = { cursor ->
                if (isRankMode) {
                    // Check if we have data for this specific date
                    val count =
                        db.trendQueries.countTrends(source.id, tab.id, targetDate).executeAsOne()
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
            lastItemMetadataExtractor = { topic ->
                topic.receiveDate to topic.rank
            },
        )

        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            remoteMediator = mediator,
            pagingSourceFactory = {
                if (isRankMode) {
                    QueryPagingSource(
                        transacter = db.trendQueries,
                        context = Dispatchers.IO,
                        queryProvider = { limit, offset ->
                            db.trendQueries.getTrendsKeyset(
                                sourceId = source.id,
                                tabId = tab.id,
                                date = targetDate,
                                offset = offset,
                                limit = limit
                            )
                        },
                        countQuery =
                            db.trendQueries.countTrends(source.id, tab.id, targetDate)
                    )
                } else {
                    QueryPagingSource(
                        transacter = db.trendQueries,
                        context = Dispatchers.IO,
                        queryProvider = { limit, offset ->
                            db.trendQueries.getRealtimeTrendsKeyset(
                                sourceId = source.id,
                                tabId = tab.id,
                                offset = offset,
                                limit = limit
                            )
                        },
                        countQuery =
                            db.trendQueries.countTrends(
                                source.id,
                                tab.id,
                                targetDate
                            ), // This count might be inaccurate for realtime but ok for invalidation
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
                    isNew = trend.isNew,
                    payload = emptyMap(),
                    receiveDate = trend.receiveDate
                )
            }
        }
    }
}
