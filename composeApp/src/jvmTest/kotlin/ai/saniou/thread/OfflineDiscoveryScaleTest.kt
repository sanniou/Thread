package ai.saniou.thread

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Comment
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.repository.GlobalSearchRepository
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals

/** Structural scale gate: result windows stay bounded while exact counts cover the full cache. */
class OfflineDiscoveryScaleTest {
    @Test
    fun largeMixedCacheReturnsBoundedTypedWindowsAndExactCounts() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        database.feedSourceQueries.insertFeedSource(
            id = "scale-reader",
            name = "Scale Reader",
            url = "https://example.test/scale.xml",
            type = "RSS",
            description = null,
            iconUrl = null,
            lastUpdate = 10_000,
            selectorConfig = "{}",
            autoRefresh = 0,
            refreshInterval = 3_600_000,
        )

        repeat(1_200) { index ->
            val id = index.toString().padStart(4, '0')
            database.topicQueries.upsertTopic(
                Topic(
                    id = "scale-topic-$id",
                    sourceId = "nmb",
                    channelId = "scale",
                    commentCount = 1,
                    authorId = "author-$id",
                    authorName = "Scale Author",
                    title = "needle topic $id",
                    content = "offline scale body",
                    summary = "bounded paging seed",
                    agreeCount = 0,
                    disagreeCount = 0,
                    isCollected = false,
                    createdAt = index.toLong(),
                    lastReplyAt = index.toLong(),
                    lastVisitedAt = null,
                    lastViewedCommentId = null,
                )
            )
            database.commentQueries.upsertComment(
                Comment(
                    id = "scale-comment-$id",
                    sourceId = "nmb",
                    topicId = "scale-topic-$id",
                    page = 1,
                    userHash = "commenter-$id",
                    admin = 0,
                    title = null,
                    createdAt = index.toLong(),
                    content = "needle comment $id",
                    authorName = "Scale Commenter",
                    floor = 1,
                    replyToId = null,
                    agreeCount = 0,
                    disagreeCount = 0,
                    subCommentCount = 0,
                    authorLevel = null,
                    isPo = false,
                )
            )
            database.articleQueries.insertArticle(
                id = "scale-article-$id",
                feedSourceId = "scale-reader",
                title = "needle article $id",
                description = "bounded reader seed",
                content = "offline scale article body",
                link = "https://example.test/articles/$id",
                author = "Scale Editor",
                publishDate = index.toLong(),
                isRead = 0,
                isBookmarked = 0,
                imageUrl = null,
                rawContent = null,
            )
        }

        val di = createAppDi(database)
        val result = di.direct.instance<GlobalSearchRepository>().search("needle", limitPerType = 7)
        assertEquals(1_200L, result.topicCount)
        assertEquals(1_200L, result.commentCount)
        assertEquals(1_200L, result.articleCount)
        assertEquals(21, result.results.size)
        assertEquals(
            mapOf(
                GlobalSearchType.TOPIC to 7,
                GlobalSearchType.COMMENT to 7,
                GlobalSearchType.ARTICLE to 7,
            ),
            result.results.groupingBy { it.type }.eachCount(),
        )
        assertEquals(result.results.size, result.results.map { "${it.type}:${it.sourceId}:${it.id}" }.toSet().size)

        di.direct.instance<ReaderRefreshScheduler>().stop()
        driver.close()
    }
}
