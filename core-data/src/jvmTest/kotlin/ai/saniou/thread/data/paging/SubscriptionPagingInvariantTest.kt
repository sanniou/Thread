package ai.saniou.thread.data.paging

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.SelectSubscriptionTopic
import ai.saniou.thread.db.table.forum.Topic
import androidx.paging.PagingSource
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SubscriptionPagingInvariantTest {
    @Test
    fun officialQueryPagingSourceReadsSubscriptionsWithoutTopicListingSideEffects() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        try {
            database.topicQueries.upsertTopic(topic("older", createdAt = 10))
            database.topicQueries.upsertTopic(topic("newer", createdAt = 20))
            database.subscriptionQueries.insertSubscription("key", "nmb", "older", 1, 10, 0)
            database.subscriptionQueries.insertSubscription("key", "nmb", "newer", 1, 20, 0)

            // No TopicListing rows are inserted: the subscription relation is the listing source.
            assertEquals(2, database.topicQueries.getAllTopics().executeAsList().size)
            val source = QueryPagingSource(
                countQuery = database.subscriptionQueries.countSubscriptionsBySubscriptionKey("key"),
                transacter = database.subscriptionQueries,
                context = ioDispatcher,
                queryProvider = { limit, offset ->
                    database.subscriptionQueries.selectSubscriptionTopic("key", limit, offset)
                },
            )
            val page = assertIs<PagingSource.LoadResult.Page<Int, SelectSubscriptionTopic>>(
                source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
            )

            assertEquals(listOf("newer", "older"), page.data.map { it.id })
            assertEquals(null, page.prevKey)
            assertEquals(null, page.nextKey)
        } finally {
            driver.close()
        }
    }

    private fun topic(id: String, createdAt: Long) = Topic(
        id = id,
        sourceId = "nmb",
        channelId = "subscription",
        commentCount = 0,
        authorId = "author",
        authorName = "Author",
        title = id,
        content = "Cached $id",
        summary = "Cached $id",
        agreeCount = null,
        disagreeCount = null,
        isCollected = null,
        createdAt = createdAt,
        lastReplyAt = createdAt,
        lastVisitedAt = null,
        lastViewedCommentId = null,
    )
}
