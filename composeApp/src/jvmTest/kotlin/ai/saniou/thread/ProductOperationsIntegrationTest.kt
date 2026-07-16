package ai.saniou.thread

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Comment
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.RefreshPolicy
import ai.saniou.thread.domain.repository.GlobalSearchRepository
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.WorkspaceSessionRepository
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductOperationsIntegrationTest {
    @Test
    fun offlineDiscoveryHealthAndVersionedSessionShareOneProductGraph() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = createDatabase(driver)
        seedSearchableContent(database)
        val di = createAppDi(database)

        val search = di.direct.instance<GlobalSearchRepository>()
            .search("跨平台", limitPerType = 10)
        assertEquals(3, search.results.size)
        assertEquals(GlobalSearchType.entries.toSet(), search.results.mapTo(mutableSetOf()) { it.type })
        assertEquals(1L, search.topicCount)
        assertEquals(1L, search.commentCount)
        assertEquals(1L, search.articleCount)

        val operations = di.direct.instance<OperationsRepository>()
        val cached = operations.observe().first { it.cachedItemCount == 3L }
        assertEquals(4, cached.sources.size)
        assertTrue(cached.sources.any { it.id == "nmb" && it.primaryItemCount == 1L && it.secondaryItemCount == 1L })
        assertTrue(cached.sources.any { it.id == "reader-news" && it.primaryItemCount == 1L })

        val coordinator = di.direct.instance<RefreshCoordinator>()
        coordinator.execute<Unit>(
            key = "forum:nmb:catalog",
            label = "A岛版块",
            policy = RefreshPolicy(maxAttempts = 1),
        ) { Result.failure(IllegalStateException("401 login expired")) }
        val degraded = operations.observe().first { it.failedRefreshCount == 1 }
        assertEquals(
            SourceOperationalState.AUTHENTICATION_REQUIRED,
            degraded.sources.first { it.id == "nmb" }.state,
        )
        operations.clearRefreshDiagnostic("nmb")
        assertEquals(0, operations.observe().first { it.failedRefreshCount == 0 }.failedRefreshCount)

        val sessions = di.direct.instance<WorkspaceSessionRepository>()
        sessions.save(WorkspaceSession(destination = WorkspaceDestination.FORUM))
        coroutineScope {
            listOf(
                async { sessions.update { it.copy(destination = WorkspaceDestination.OPERATIONS) } },
                async { sessions.update { it.copy(globalSearchQuery = "跨平台") } },
                async { sessions.update { it.copy(forumSourceId = "nmb") } },
            ).awaitAll()
        }
        val restored = sessions.get()
        assertEquals(WorkspaceDestination.OPERATIONS, restored.destination)
        assertEquals("跨平台", restored.globalSearchQuery)
        assertEquals("nmb", restored.forumSourceId)
        assertEquals(WorkspaceSession.CURRENT_VERSION, restored.version)

        di.direct.instance<ReaderRefreshScheduler>().stop()
        driver.close()
    }

    private suspend fun seedSearchableContent(database: Database) {
        database.topicQueries.upsertTopic(
            Topic(
                id = "topic-alpha",
                sourceId = "nmb",
                channelId = "main",
                commentCount = 1,
                authorId = "po",
                authorName = "作者",
                title = "跨平台分页设计",
                content = "SQLDelight 缓存主题正文",
                summary = "论坛搜索摘要",
                agreeCount = 2,
                disagreeCount = 0,
                isCollected = false,
                createdAt = 1_000,
                lastReplyAt = 2_000,
                lastVisitedAt = null,
                lastViewedCommentId = null,
            )
        )
        database.commentQueries.upsertComment(
            Comment(
                id = "reply-alpha",
                sourceId = "nmb",
                topicId = "topic-alpha",
                page = 1,
                userHash = "reply-user",
                admin = 0,
                title = null,
                createdAt = 2_000,
                content = "跨平台回复也能在离线状态搜索",
                authorName = "回复者",
                floor = 2,
                replyToId = null,
                agreeCount = 1,
                disagreeCount = 0,
                subCommentCount = 0,
                authorLevel = null,
                isPo = false,
            )
        )
        database.feedSourceQueries.insertFeedSource(
            id = "reader-news",
            name = "Reader News",
            url = "https://example.test/feed.xml",
            type = "RSS",
            description = null,
            iconUrl = null,
            lastUpdate = 3_000,
            selectorConfig = "{}",
            autoRefresh = 1,
            refreshInterval = 3_600_000,
        )
        database.articleQueries.insertArticle(
            id = "article-alpha",
            feedSourceId = "reader-news",
            title = "Compose 跨平台阅读器",
            description = "Reader 搜索摘要",
            content = "跨平台文章正文",
            link = "https://example.test/article",
            author = "编辑部",
            publishDate = 3_000,
            isRead = 0,
            isBookmarked = 0,
            imageUrl = null,
            rawContent = null,
        )
    }
}
