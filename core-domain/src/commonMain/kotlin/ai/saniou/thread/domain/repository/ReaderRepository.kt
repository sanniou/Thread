package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.reader.Article
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.ArticleWithSource
import ai.saniou.thread.domain.model.reader.ReaderRefreshReport
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface ReaderRepository {
    // Feed Sources
    fun getAllFeedSources(): Flow<List<FeedSource>>
    suspend fun getFeedSource(id: String): FeedSource?
    suspend fun addFeedSource(feedSource: FeedSource)
    suspend fun updateFeedSource(feedSource: FeedSource)
    suspend fun deleteFeedSource(id: String)

    // Articles
    fun getArticlesPaging(
        feedSourceId: String? = null,
        query: String = "",
        isRead: Boolean? = null,
        isBookmarked: Boolean? = null
    ): Flow<PagingData<Article>>
    suspend fun getArticle(id: String): Article?
    suspend fun markArticleAsRead(id: String, isRead: Boolean)
    suspend fun markArticleAsBookmarked(id: String, isBookmarked: Boolean)
    fun getArticleCounts(feedSourceId: String): Flow<Pair<Int, Int>>
    suspend fun getRecentArticles(limit: Long, offset: Long): List<ArticleWithSource>

    // Sync/Refresh
    suspend fun refreshAllFeeds(): ReaderRefreshReport
    suspend fun refreshFeed(feedSourceId: String): Result<Unit>
}
