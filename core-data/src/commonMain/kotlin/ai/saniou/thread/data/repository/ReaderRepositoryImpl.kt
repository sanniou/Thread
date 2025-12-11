package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.parser.FeedParserFactory
import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.model.FeedSource
import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.repository.ReaderRepository
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

class ReaderRepositoryImpl(
    private val db: Database,
    private val parserFactory: FeedParserFactory,
    private val httpClient: HttpClient
) : ReaderRepository {

    override fun getAllFeedSources(): Flow<List<FeedSource>> {
        return db.feedSourceQueries.getAllFeedSources()
            .asFlow()
            .mapToList(Dispatchers.IO)
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
                        } ?: emptyMap()
                    )
                }
            }
    }

    override suspend fun getFeedSource(id: String): FeedSource? {
        return withContext(Dispatchers.IO) {
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
                        } ?: emptyMap()
                    )
            }
        }
    }

    override suspend fun addFeedSource(feedSource: FeedSource) {
        withContext(Dispatchers.IO) {
            db.feedSourceQueries.insertFeedSource(
                id = feedSource.id,
                name = feedSource.name,
                url = feedSource.url,
                type = feedSource.type.name,
                description = feedSource.description,
                iconUrl = feedSource.iconUrl,
                lastUpdate = feedSource.lastUpdate,
                selectorConfig = Json.encodeToString(feedSource.selectorConfig)
            )
        }
    }

    override suspend fun updateFeedSource(feedSource: FeedSource) {
        addFeedSource(feedSource)
    }

    override suspend fun deleteFeedSource(id: String) {
        withContext(Dispatchers.IO) {
            db.feedSourceQueries.deleteFeedSource(id)
        }
    }

    override fun getArticlesPaging(feedSourceId: String?, query: String): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = db.articleQueries.countArticles(feedSourceId, query),
                    transacter = db.articleQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        db.articleQueries.getArticlesPaging(feedSourceId, query, limit, offset)
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
        return withContext(Dispatchers.IO) {
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
        withContext(Dispatchers.IO) {
            db.articleQueries.markArticleAsRead(if (isRead) 1L else 0L, id)
        }
    }

    override suspend fun markArticleAsBookmarked(id: String, isBookmarked: Boolean) {
         withContext(Dispatchers.IO) {
            db.articleQueries.markArticleAsBookmarked(if (isBookmarked) 1L else 0L, id)
        }
    }

    override suspend fun refreshAllFeeds() {
        val sources = withContext(Dispatchers.IO) {
             db.feedSourceQueries.getAllFeedSources().executeAsList()
        }
        sources.forEach { sourceEntity ->
             refreshFeed(sourceEntity.id)
        }
    }

    override suspend fun refreshFeed(feedSourceId: String) {
        val source = getFeedSource(feedSourceId) ?: return

        try {
            val response = httpClient.get(source.url)
            val content = response.bodyAsText()

            val parser = parserFactory.getParser(source.type)
            val articles = parser.parse(source, content)

            withContext(Dispatchers.IO) {
                db.transaction {
                    articles.forEach { article ->
                        val existing = db.articleQueries.getArticleById(article.id).executeAsOneOrNull()
                        val isRead = existing?.isRead ?: (if (article.isRead) 1L else 0L)
                        val isBookmarked = existing?.isBookmarked ?: (if (article.isBookmarked) 1L else 0L)

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
