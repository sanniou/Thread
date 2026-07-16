package ai.saniou.thread

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.source.SourceCatalog
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.database.createDatabase
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Topic
import ai.saniou.thread.db.table.forum.TopicListing
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.kodein.di.direct
import org.kodein.di.instance

@OptIn(ExperimentalVoyagerApi::class)
fun main(args: Array<String>) {
    if ("--smoke-check" in args) {
        runDesktopStartupProbe()
        return
    }
    application {
        addTempDirectoryRemovalHook()
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1240.dp, 820.dp),
        )
        val attachmentPicker = remember { DesktopAttachmentPicker() }
        Window(
            onCloseRequest = ::exitApplication,
            title = "Thread · Forum & Reader",
            undecorated = false,
            state = windowState
        ) {
            App(attachmentPicker = attachmentPicker)
        }
    }
}

internal fun runDesktopStartupProbe() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.synchronous().create(driver)
    val database = createDatabase(driver)
    runBlocking { seedOfflineWorkspace(database) }
    val di = createAppDi(database)
    try {
        val catalog = di.direct.instance<SourceCatalog>()
        val sourceIds = catalog.descriptors.value.mapTo(sortedSetOf()) { it.id }
        require(sourceIds.containsAll(listOf("nmb", "tieba", "discourse"))) {
            "Desktop runtime catalog is incomplete: $sourceIds"
        }
        runBlocking {
            val article = requireNotNull(di.direct.instance<ReaderRepository>().getArticle("probe-article"))
            require(article.isRead && article.isBookmarked && article.content == "Available offline")
            val topic = di.direct.instance<SourceCache>().observeTopic("tieba", "probe-topic").first()
            require(topic.title == "Cached forum topic" && topic.content == "Available offline")
        }
        di.direct.instance<ReaderRefreshScheduler>().stop()
        println("Thread Desktop offline startup probe passed: ${sourceIds.joinToString()}")
    } finally {
        driver.close()
    }
}

private suspend fun seedOfflineWorkspace(database: Database) {
    database.keyValueQueries.insertKeyValue("current_source_id", "tieba")
    database.feedSourceQueries.insertFeedSource(
        id = "probe-feed",
        name = "Offline Feed",
        url = "https://fixture.thread.test/feed.xml",
        type = "RSS",
        description = "Seeded by the startup probe",
        iconUrl = null,
        lastUpdate = 1,
        selectorConfig = "{}",
        autoRefresh = 0,
        refreshInterval = 3_600_000,
    )
    database.articleQueries.insertArticle(
        id = "probe-article",
        feedSourceId = "probe-feed",
        title = "Cached reader article",
        description = "Offline summary",
        content = "Available offline",
        link = "https://fixture.thread.test/article",
        author = "Thread",
        publishDate = 2,
        isRead = 1,
        isBookmarked = 1,
        imageUrl = null,
        rawContent = "<p>Available offline</p>",
    )
    database.topicQueries.upsertTopic(
        Topic(
            id = "probe-topic",
            sourceId = "tieba",
            channelId = "probe-channel",
            commentCount = 0,
            authorId = "probe-author",
            authorName = "Thread",
            title = "Cached forum topic",
            content = "Available offline",
            summary = "Offline summary",
            agreeCount = 1,
            disagreeCount = 0,
            isCollected = false,
            createdAt = 1,
            lastReplyAt = 2,
            lastVisitedAt = null,
            lastViewedCommentId = null,
        )
    )
    database.topicQueries.upsertTopicListing(
        TopicListing(
            sourceId = "tieba",
            topicId = "probe-topic",
            listType = "channel",
            listId = "probe-channel",
            page = 1,
            receiveDate = 2,
            receiveOrder = 1,
        )
    )
}
