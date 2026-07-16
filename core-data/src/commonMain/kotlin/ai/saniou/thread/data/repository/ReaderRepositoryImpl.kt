package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.data.parser.FeedParserFactory
import ai.saniou.thread.data.reader.ReaderSubscriptionCodec
import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import ai.saniou.thread.domain.model.reader.ArticleWithSource
import ai.saniou.thread.domain.model.reader.ReaderRefreshReport
import ai.saniou.thread.domain.model.reader.ReaderImportReport
import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.data.cache.CacheFreshnessStore
import ai.saniou.thread.domain.cache.CachePolicyProvider
import ai.saniou.thread.domain.cache.CacheResource
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.paging3.QueryPagingSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class ReaderRepositoryImpl(
    private val db: Database,
    private val parserFactory: FeedParserFactory,
    private val httpClient: HttpClient,
    private val refreshCoordinator: RefreshCoordinator,
    private val freshnessStore: CacheFreshnessStore,
    private val cachePolicyProvider: CachePolicyProvider,
    private val subscriptionCodec: ReaderSubscriptionCodec,
) : ReaderRepository {

    override fun getAllFeedSources(): Flow<List<FeedSource>> {
        return db.feedSourceQueries.getAllFeedSources()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map { entity ->
                    FeedSource(
                        id = entity.id,
                        name = entity.name,
                        url = entity.url,
                        type = FeedType.valueOf(entity.type),
                        description = entity.description,
                        iconUrl = entity.iconUrl,
                        lastUpdate = entity.lastUpdate,
                        selectorConfig = entity.selectorConfig?.let {
                            try {
                                Json.decodeFromString<Map<String, String>>(it)
                            } catch (e: Exception) {
                                emptyMap()
                            }
                        } ?: emptyMap(),
                        autoRefresh = entity.autoRefresh == 1L,
                        refreshInterval = entity.refreshInterval
                    )
                }
            }
    }

    override suspend fun getFeedSource(id: String): FeedSource? {
        return withContext(ioDispatcher) {
            db.feedSourceQueries.getFeedSourceById(id).executeAsOneOrNull()?.let { entity ->
                 FeedSource(
                        id = entity.id,
                        name = entity.name,
                        url = entity.url,
                        type = FeedType.valueOf(entity.type),
                        description = entity.description,
                        iconUrl = entity.iconUrl,
                        lastUpdate = entity.lastUpdate,
                        selectorConfig = entity.selectorConfig?.let {
                            try {
                                Json.decodeFromString<Map<String, String>>(it)
                            } catch (e: Exception) {
                                emptyMap()
                            }
                        } ?: emptyMap(),
                        autoRefresh = entity.autoRefresh == 1L,
                        refreshInterval = entity.refreshInterval
                    )
            }
        }
    }

    override suspend fun addFeedSource(feedSource: FeedSource) {
        validateFeedSource(feedSource)
        withContext(ioDispatcher) {
            db.feedSourceQueries.insertFeedSource(
                id = feedSource.id,
                name = feedSource.name,
                url = feedSource.url,
                type = feedSource.type.name,
                description = feedSource.description,
                iconUrl = feedSource.iconUrl,
                lastUpdate = feedSource.lastUpdate,
                selectorConfig = Json.encodeToString(feedSource.selectorConfig),
                autoRefresh = if (feedSource.autoRefresh) 1L else 0L,
                refreshInterval = feedSource.refreshInterval
            )
        }
    }

    override suspend fun updateFeedSource(feedSource: FeedSource) {
        addFeedSource(feedSource)
    }

    override suspend fun deleteFeedSource(id: String) {
        withContext(ioDispatcher) {
            db.feedSourceQueries.deleteFeedSource(id)
        }
        freshnessStore.invalidate(CacheFreshnessStore.reader(id))
    }

    override fun getArticlesPaging(
        feedSourceId: String?,
        query: String,
        isRead: Boolean?,
        isBookmarked: Boolean?
    ): Flow<PagingData<Article>> {
        return Pager(
            config = threadPagingConfig(),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.articleQueries.countArticles(
                        feedSourceId = feedSourceId,
                        query = query,
                        isRead = isRead?.let { if (it) 1L else 0L },
                        isBookmarked = isBookmarked?.let { if (it) 1L else 0L }
                    ),
                    transacter = db.articleQueries,
                    context = ioDispatcher,
                    queryProvider = { limit, offset ->
                        db.articleQueries.getArticlesPaging(
                            feedSourceId = feedSourceId,
                            query = query,
                            isRead = isRead?.let { if (it) 1L else 0L },
                            isBookmarked = isBookmarked?.let { if (it) 1L else 0L },
                            limit = limit,
                            offset = offset
                        )
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                Article(
                    id = entity.id,
                    feedSourceId = entity.feedSourceId,
                    title = entity.title,
                    description = entity.description,
                    content = entity.content,
                    link = entity.link,
                    author = entity.author,
                    publishDate = Instant.fromEpochMilliseconds(entity.publishDate),
                    isRead = entity.isRead == 1L,
                    isBookmarked = entity.isBookmarked == 1L,
                    imageUrl = entity.imageUrl,
                    rawContent = entity.rawContent
                )
            }
        }
    }

    override suspend fun getArticle(id: String): Article? {
        return withContext(ioDispatcher) {
            db.articleQueries.getArticleById(id).executeAsOneOrNull()?.let { entity ->
                Article(
                    id = entity.id,
                    feedSourceId = entity.feedSourceId,
                    title = entity.title,
                    description = entity.description,
                    content = entity.content,
                    link = entity.link,
                    author = entity.author,
                    publishDate = Instant.fromEpochMilliseconds(entity.publishDate),
                    isRead = entity.isRead == 1L,
                    isBookmarked = entity.isBookmarked == 1L,
                    imageUrl = entity.imageUrl,
                    rawContent = entity.rawContent
                )
            }
        }
    }

    override suspend fun markArticleAsRead(id: String, isRead: Boolean) {
        withContext(ioDispatcher) {
            val article = db.articleQueries.getArticleById(id).executeAsOneOrNull() ?: return@withContext
            val state = db.articleQueries.getArticleUserState(id).executeAsOneOrNull()
            val bookmarked = state?.isBookmarked ?: article.isBookmarked
            db.transaction {
                db.articleQueries.markArticleAsRead(if (isRead) 1L else 0L, id)
                db.articleQueries.upsertArticleUserState(
                    articleId = id,
                    isRead = if (isRead) 1L else 0L,
                    isBookmarked = bookmarked,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    override suspend fun markArticleAsBookmarked(id: String, isBookmarked: Boolean) {
         withContext(ioDispatcher) {
            val article = db.articleQueries.getArticleById(id).executeAsOneOrNull() ?: return@withContext
            val state = db.articleQueries.getArticleUserState(id).executeAsOneOrNull()
            val read = state?.isRead ?: article.isRead
            db.transaction {
                db.articleQueries.markArticleAsBookmarked(if (isBookmarked) 1L else 0L, id)
                db.articleQueries.upsertArticleUserState(
                    articleId = id,
                    isRead = read,
                    isBookmarked = if (isBookmarked) 1L else 0L,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
         }
    }

    override fun getArticleCounts(feedSourceId: String): Flow<Pair<Int, Int>> {
        val totalFlow = db.articleQueries.countTotalArticlesByFeedSource(feedSourceId)
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }

        val unreadFlow = db.articleQueries.countUnreadArticlesByFeedSource(feedSourceId)
            .asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }

        return combine(totalFlow, unreadFlow) { total, unread ->
            total to unread
        }
    }

    override suspend fun getRecentArticles(limit: Long, offset: Long): List<ArticleWithSource> {
        require(limit > 0) { "limit must be positive" }
        require(offset >= 0) { "offset must not be negative" }
        return withContext(ioDispatcher) {
            db.articleQueries.getRecentArticlesWithSource(limit, offset).executeAsList().map { row ->
                ArticleWithSource(
                    article = Article(
                        id = row.id,
                        feedSourceId = row.feedSourceId,
                        title = row.title,
                        description = row.description,
                        content = row.content,
                        link = row.link,
                        author = row.author,
                        publishDate = Instant.fromEpochMilliseconds(row.publishDate),
                        isRead = row.isRead == 1L,
                        isBookmarked = row.isBookmarked == 1L,
                        imageUrl = row.imageUrl,
                        rawContent = row.rawContent,
                    ),
                    sourceName = row.sourceName,
                    sourceIconUrl = row.sourceIconUrl,
                )
            }
        }
    }

    override suspend fun refreshAllFeeds(): ReaderRefreshReport = supervisorScope {
        val sources = withContext(ioDispatcher) {
             db.feedSourceQueries.getAllFeedSources().executeAsList()
        }
        val results = sources.map { sourceEntity ->
            async { sourceEntity.id to refreshFeed(sourceEntity.id) }
        }.awaitAll()

        ReaderRefreshReport(
            refreshedSourceIds = results.mapNotNullTo(mutableSetOf()) { (id, result) ->
                id.takeIf { result.isSuccess }
            },
            failures = results.mapNotNull { (id, result) ->
                result.exceptionOrNull()?.let { error ->
                    id to (error.message ?: error::class.simpleName ?: "Unknown error")
                }
            }.toMap(),
        )
    }

    override suspend fun refreshFeed(feedSourceId: String, forceRefresh: Boolean): Result<Unit> {
        val source = getFeedSource(feedSourceId)
            ?: return Result.failure(IllegalArgumentException("Feed source not found: $feedSourceId"))
        val freshnessKey = CacheFreshnessStore.reader(feedSourceId)
        val basePolicy = cachePolicyProvider.policy(feedSourceId, CacheResource.READER_FEED)
        val policy = basePolicy.copy(
            freshness = minOf(basePolicy.freshness, source.refreshInterval.coerceAtLeast(60_000L).milliseconds),
        )
        val hasCachedArticles = withContext(ioDispatcher) {
            db.articleQueries.countTotalArticlesByFeedSource(feedSourceId).executeAsOne() > 0L
        }
        if (!forceRefresh && hasCachedArticles && freshnessStore.isFresh(freshnessKey, policy)) {
            return Result.success(Unit)
        }
        val result = refreshCoordinator.execute(
            key = "reader:$feedSourceId",
            label = source.name,
        ) {
            runCatching {
            val response = httpClient.get(source.url)
            check(response.status.isSuccess()) { "Feed request failed: ${response.status.value}" }
            val content = response.bodyAsText()

            val parser = parserFactory.getParser(source.type)
            val articles = parser.parse(source, content)
            require(articles.isNotEmpty()) { "Feed contains no valid articles" }

            withContext(ioDispatcher) {
                db.transaction {
                    articles.forEach { article ->
                        val existing = db.articleQueries.getArticleById(article.id).executeAsOneOrNull()
                        val userState = db.articleQueries.getArticleUserState(article.id).executeAsOneOrNull()
                        val isRead = userState?.isRead ?: existing?.isRead ?: (if (article.isRead) 1L else 0L)
                        val isBookmarked = userState?.isBookmarked ?: existing?.isBookmarked
                            ?: (if (article.isBookmarked) 1L else 0L)

                        db.articleQueries.insertArticle(
                            id = article.id,
                            feedSourceId = article.feedSourceId,
                            title = article.title,
                            description = article.description,
                            content = article.content,
                            link = article.link,
                            author = article.author,
                            publishDate = article.publishDate.toEpochMilliseconds(),
                            isRead = isRead,
                            isBookmarked = isBookmarked,
                            imageUrl = article.imageUrl,
                            rawContent = article.rawContent
                        )
                    }
                    db.feedSourceQueries.updateLastUpdate(Clock.System.now().toEpochMilliseconds(), feedSourceId)
                }
            }
            }
        }
        if (result.isSuccess) freshnessStore.markFresh(freshnessKey)
        return result
    }

    override suspend fun exportSubscriptions(format: ReaderSubscriptionFormat): Result<String> = runCatching {
        val sources = getAllFeedSources().first()
        subscriptionCodec.encode(sources, format)
    }

    override suspend fun importSubscriptions(
        payload: String,
        format: ReaderSubscriptionFormat,
    ): Result<ReaderImportReport> = runCatching {
        val imported = subscriptionCodec.decode(payload, format)
        var added = 0
        var updated = 0
        var skipped = 0
        val ids = mutableSetOf<String>()
        withContext(ioDispatcher) {
            db.transaction {
                imported.forEach { candidate ->
                    val existing = db.feedSourceQueries.getFeedSourceById(candidate.id).executeAsOneOrNull()
                        ?: db.feedSourceQueries.getFeedSourceByUrl(candidate.url).executeAsOneOrNull()
                    val source = candidate.copy(
                        id = existing?.id ?: candidate.id,
                        lastUpdate = existing?.lastUpdate ?: candidate.lastUpdate,
                    )
                    ids += source.id
                    if (existing == null) {
                        insertFeedSource(source)
                        added++
                    } else if (existing.matches(source)) {
                        skipped++
                    } else {
                        insertFeedSource(source)
                        updated++
                    }
                }
            }
        }
        ReaderImportReport(added, updated, skipped, ids)
    }

    private suspend fun insertFeedSource(feedSource: FeedSource) {
        db.feedSourceQueries.insertFeedSource(
            id = feedSource.id,
            name = feedSource.name,
            url = feedSource.url,
            type = feedSource.type.name,
            description = feedSource.description,
            iconUrl = feedSource.iconUrl,
            lastUpdate = feedSource.lastUpdate,
            selectorConfig = Json.encodeToString(feedSource.selectorConfig),
            autoRefresh = if (feedSource.autoRefresh) 1L else 0L,
            refreshInterval = feedSource.refreshInterval,
        )
    }

    private fun ai.saniou.thread.db.table.reader.FeedSourceEntity.matches(source: FeedSource): Boolean =
        name == source.name && url == source.url && type == source.type.name &&
            description == source.description && iconUrl == source.iconUrl &&
            selectorConfig == Json.encodeToString(source.selectorConfig) &&
            autoRefresh == (if (source.autoRefresh) 1L else 0L) && refreshInterval == source.refreshInterval

    private fun validateFeedSource(source: FeedSource) {
        require(source.id.isNotBlank()) { "Feed source id must not be blank" }
        require(source.name.isNotBlank()) { "Feed source name must not be blank" }
        require(source.url.startsWith("https://") || source.url.startsWith("http://")) {
            "Feed source URL must use http:// or https://"
        }
        require(source.refreshInterval >= 60_000L) { "Refresh interval must be at least one minute" }
        if (source.type != FeedType.RSS) {
            require(source.selectorConfig.isNotEmpty()) { "${source.type} feed requires selector configuration" }
        }
    }
}
