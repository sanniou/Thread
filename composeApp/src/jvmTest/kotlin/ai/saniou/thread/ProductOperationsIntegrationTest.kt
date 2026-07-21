package ai.saniou.thread

import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Comment
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.domain.model.operations.SourceOperationalState
import ai.saniou.thread.domain.model.search.GlobalSearchType
import ai.saniou.thread.domain.model.workspace.WorkspaceDestination
import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.model.workspace.ListAnchor
import ai.saniou.thread.domain.model.workspace.RestorableContentKind
import ai.saniou.thread.domain.model.workspace.RestorableContentReference
import ai.saniou.thread.domain.model.workspace.ReaderWorkspaceState
import ai.saniou.thread.domain.model.operations.ProductCommandAction
import ai.saniou.thread.domain.model.activity.IdentityValidity
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.repository.ActivityCenterRepository
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.refresh.RefreshPolicy
import ai.saniou.thread.domain.refresh.RefreshHistoryRepository
import ai.saniou.thread.domain.repository.GlobalSearchRepository
import ai.saniou.thread.domain.repository.OperationsRepository
import ai.saniou.thread.domain.repository.WorkspaceSessionRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.WorkspaceRestorationRepository
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.domain.usecase.operations.BuildProductCommandsUseCase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

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
        assertEquals(4, search.results.size)
        assertEquals(GlobalSearchType.entries.toSet(), search.results.mapTo(mutableSetOf()) { it.type })
        assertEquals(1L, search.topicCount)
        assertEquals(1L, search.commentCount)
        assertEquals(1L, search.articleCount)
        assertEquals(1L, search.socialCount)

        val operations = di.direct.instance<OperationsRepository>()
        val cached = operations.observe().awaitState("cached source health") { it.cachedItemCount == 3L }
        assertEquals(4, cached.sources.size)
        assertTrue(cached.sources.any { it.id == "nmb" && it.primaryItemCount == 1L && it.secondaryItemCount == 1L })
        assertTrue(cached.sources.any { it.id == "reader-news" && it.primaryItemCount == 1L })
        val activityCenter = di.direct.instance<ActivityCenterRepository>()
        val activity = activityCenter.observe().awaitState("activity source projection") {
            it.operations.cachedItemCount == 3L
        }
        val commands = di.direct.instance<BuildProductCommandsUseCase>()(activity)
        assertTrue(commands.any { it.request?.type == ProductActionType.REFRESH_ALL_READERS })
        assertTrue(commands.any { it.sourceId == "nmb" && it.request?.type == ProductActionType.REFRESH_SOURCE })
        assertTrue(commands.any { it.sourceId == "nmb" && it.request?.type == ProductActionType.SET_SOURCE_ENABLED })
        assertTrue(commands.any { it.action == ProductCommandAction.OPEN_ACTIVITY_CENTER })

        val restoration = di.direct.instance<WorkspaceRestorationRepository>()
        assertTrue(restoration.isAvailable(RestorableContentReference(
            RestorableContentKind.TOPIC, "topic-alpha", "nmb", WorkspaceDestination.FORUM,
        )))
        assertTrue(restoration.isAvailable(RestorableContentReference(
            RestorableContentKind.ARTICLE, "article-alpha", workspace = WorkspaceDestination.READER,
        )))
        assertFalse(restoration.isAvailable(RestorableContentReference(
            RestorableContentKind.ARTICLE, "missing", workspace = WorkspaceDestination.READER,
        )))

        val coordinator = di.direct.instance<RefreshCoordinator>()
        val settings = di.direct.instance<SettingsRepository>()
        settings.saveValue("test.secret", "webdav-password-never-export")
        coordinator.execute<Unit>(
            key = "forum:nmb:catalog",
            label = "A岛版块",
            policy = RefreshPolicy(maxAttempts = 1),
        ) {
            Result.failure(IllegalStateException(
                "401 login expired token=super-secret-123 cookie=session-secret https://example.test/?api_key=query-secret"
            ))
        }
        val durableFailure = di.direct.instance<RefreshHistoryRepository>().observe().first()
            .getValue("forum:nmb:catalog")
        assertEquals(1, durableFailure.consecutiveFailureCount)
        val degraded = operations.observe().first()
        assertEquals(1, degraded.failedRefreshCount)
        assertEquals(
            SourceOperationalState.AUTHENTICATION_REQUIRED,
            degraded.sources.first { it.id == "nmb" }.state,
        )
        assertEquals(1, degraded.sources.first { it.id == "nmb" }.consecutiveFailureCount)
        assertEquals(
            IdentityValidity.EXPIRED,
            activityCenter.observe().awaitState("explicit expired identity") {
                it.identities.any { identity -> identity.sourceId == "nmb" && identity.validity == IdentityValidity.EXPIRED }
            }.identities.first { it.sourceId == "nmb" }.validity,
        )
        val diagnostic = withTimeout(10_000) { operations.exportDiagnostic() }
        assertTrue(diagnostic.redacted)
        assertTrue("[REDACTED]" in diagnostic.payload)
        listOf("super-secret-123", "session-secret", "query-secret", "webdav-password-never-export")
            .forEach { secret -> assertFalse(secret in diagnostic.payload, "Diagnostic leaked $secret") }
        operations.clearRefreshDiagnostic("nmb")
        assertEquals(
            0,
            operations.observe().awaitState("cleared durable diagnostic") { it.failedRefreshCount == 0 }.failedRefreshCount,
        )

        val sessions = di.direct.instance<WorkspaceSessionRepository>()
        settings.saveValue(
            "workspace_session_v1",
            """{"version":1,"destination":"reader","forumSourceId":"tieba","globalSearchQuery":"legacy"}""",
        )
        val migrated = sessions.get()
        assertEquals(WorkspaceSession.CURRENT_VERSION, migrated.version)
        assertEquals(WorkspaceDestination.READER, migrated.destination)
        assertEquals("tieba", migrated.forum.sourceId)
        sessions.save(WorkspaceSession(destination = WorkspaceDestination.FORUM))
        coroutineScope {
            listOf(
                async { sessions.update { it.copy(destination = WorkspaceDestination.OPERATIONS) } },
                async {
                    sessions.update {
                        it.copy(
                            globalSearchQuery = "跨平台",
                            reader = ReaderWorkspaceState(
                                feedSourceId = "reader-news",
                                articleFilter = "UNREAD",
                                searchQuery = "Compose",
                                listAnchor = ListAnchor("reader-news:UNREAD:Compose", 42, 8),
                            ),
                        )
                    }
                },
                async { sessions.update { it.copy(forumSourceId = "nmb") } },
            ).awaitAll()
        }
        val restored = sessions.get()
        assertEquals(WorkspaceDestination.OPERATIONS, restored.destination)
        assertEquals("跨平台", restored.globalSearchQuery)
        assertEquals("nmb", restored.forumSourceId)
        assertEquals(42, restored.reader.listAnchor?.index)
        assertEquals(WorkspaceSession.CURRENT_VERSION, restored.version)

        di.direct.instance<ReaderRefreshScheduler>().stop()
        driver.close()
    }

    private suspend fun <T> Flow<T>.awaitState(label: String, predicate: suspend (T) -> Boolean): T =
        runCatching { withTimeout(10_000) { first(predicate) } }
            .getOrElse { error -> throw AssertionError("Timed out waiting for $label", error) }

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
        database.socialQueries.upsertSocialSource(
            id = "activitypub-demo",
            displayName = "Demo Social",
            baseUrl = "https://social.example.test",
            enabled = 1,
            lastSyncAt = 4_000,
        )
        database.socialQueries.upsertSocialPost(
            id = "status-alpha",
            sourceId = "activitypub-demo",
            authorId = "alice",
            authorName = "Alice",
            authorHandle = "@alice@social.example.test",
            authorAvatarUrl = null,
            authorVerified = 0,
            body = "跨平台社交时间线也能离线搜索",
            createdAt = 4_000,
            contentWarning = null,
            mediaJson = "[]",
            interactionCountsJson = "{}",
            permittedInteractionsJson = "[]",
            activeInteractionsJson = "[]",
            canonicalUrl = "https://social.example.test/@alice/status-alpha",
            replyToId = null,
            repostOfId = null,
        )
    }
}
