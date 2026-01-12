package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.trend.TrendRemoteMediator
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.GetTrendsWithTopic
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
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

        val mediator = TrendRemoteMediator(
            db = db,
            source = source,
            tab = tab,
            params = params
        )

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val targetDate = today.minus(params.dayOffset, DateTimeUnit.DAY).toString()

        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            remoteMediator = mediator.mediator,
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.trendQueries.countTrends(sourceId, tab.id, targetDate),
                    transacter = db.trendQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        db.trendQueries.getTrendsWithTopic(sourceId, tab.id, targetDate, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { trend ->
                TrendItem(
                    id = trend.topicId, // Use topicId as the main ID for UI
                    sourceId = trend.sourceId,
                    title = trend.title,
                    contentPreview = trend.contentPreview,
                    rank = trend.rank.toInt(),
                    hotness = trend.hotness,
                    channel = trend.channel,
                    author = trend.author,
                    url = trend.url,
                    isNew = trend.isNew,
                    payload = emptyMap() // TODO: Deserialize payload if needed
                )
            }
        }
    }
}
