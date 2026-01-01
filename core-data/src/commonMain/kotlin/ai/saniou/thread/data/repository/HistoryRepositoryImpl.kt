package ai.saniou.thread.data.repository

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.history.HistoryArticle
import ai.saniou.thread.domain.model.history.HistoryItem
import ai.saniou.thread.domain.model.history.HistoryPost
import ai.saniou.thread.domain.repository.HistoryRepository
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class HistoryRepositoryImpl(
    private val db: Database,
) : HistoryRepository {

    override fun getHistory(typeFilter: String?): Flow<PagingData<HistoryItem>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                if (typeFilter != null) {
                    QueryPagingSource(
                        transacter = db.historyQueries,
                        context = Dispatchers.Default,
                        countQuery = db.historyQueries.countHistoryByType(typeFilter),
                        queryProvider = { limit, offset ->
                            db.historyQueries.getHistoryIdsByType(typeFilter, limit, offset)
                        }
                    )
                } else {
                    QueryPagingSource(
                        transacter = db.historyQueries,
                        context = Dispatchers.Default,
                        countQuery = db.historyQueries.countAllHistory(),
                        queryProvider = db.historyQueries::getAllHistoryIds
                    )
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { history ->
                val accessTime = Instant.fromEpochMilliseconds(history.accessTime)
                when (history.type) {
                    "post" -> {
                        // Fetch Topic details (原 Post)
                        val post = db.topicQueries.getTopic(history.sourceId, history.itemId)
                            .executeAsOneOrNull()?.let { entity ->
                                // 需要传入 imageQueries
                                entity.toDomain(db.imageQueries)
                            }
                        if (post != null) {
                            HistoryPost(post, accessTime)
                        } else {
                            throw IllegalStateException("Post not found for history item: ${history.itemId}")
                        }
                    }

                    "article" -> {
                        val article = db.articleQueries.getArticleById(history.itemId)
                            .executeAsOneOrNull()?.toDomain()
                        if (article != null) {
                            HistoryArticle(article, accessTime)
                        } else {
                            throw IllegalStateException("Article not found for history item: ${history.itemId}")
                        }
                    }

                    else -> throw IllegalArgumentException("Unknown history type: ${history.type}")
                }
            }
        }
    }

    override suspend fun addToHistory(item: HistoryItem) {
        withContext(Dispatchers.Default) {
            val now = Clock.System.now().toEpochMilliseconds()
            when (item) {
                is HistoryPost -> {
                    db.historyQueries.insertHistory(
                        type = "post",
                        itemId = item.post.id,
                        sourceId = item.post.sourceName,
                        accessTime = now
                    )
                }

                is HistoryArticle -> {
                    db.historyQueries.insertHistory(
                        type = "article",
                        itemId = item.article.id,
                        sourceId = item.article.feedSourceId, // Use feedSourceId as sourceId for articles
                        accessTime = now
                    )
                }
            }
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.Default) {
            db.historyQueries.clearHistory()
        }
    }
}
