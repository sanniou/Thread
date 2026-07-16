package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toEntity
import ai.saniou.thread.data.sync.ArticleStateSnapshot
import ai.saniou.thread.data.sync.UserDataBundle
import ai.saniou.thread.data.sync.toDomain
import ai.saniou.thread.data.sync.toSnapshot
import ai.saniou.thread.data.sync.webdav.UserDataRemoteTransport
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.BookmarkTag
import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import ai.saniou.thread.domain.model.sync.SyncContentSummary
import ai.saniou.thread.domain.model.sync.UserDataExport
import ai.saniou.thread.domain.model.sync.UserDataImportReport
import ai.saniou.thread.domain.model.sync.WebDavConfig
import ai.saniou.thread.domain.repository.SyncRepository
import ai.saniou.thread.domain.source.SourceCatalog
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class SyncRepositoryImpl(
    private val database: Database,
    private val sourceCatalog: SourceCatalog,
    private val webDav: UserDataRemoteTransport,
) : SyncRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportUserData(): Result<UserDataExport> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val bundle = withContext(ioDispatcher) { collectBundle(now) }
        UserDataExport(
            payload = json.encodeToString(bundle),
            exportedAtEpochMillis = now,
            summary = bundle.summary(),
        )
    }

    override suspend fun importUserData(payload: String): Result<UserDataImportReport> = runCatching {
        val bundle = json.decodeFromString<UserDataBundle>(payload)
        val descriptors = bundle.sources.map { it.toDomain() }
        val feedSources = bundle.feedSources.map { it.toDomain() }
        val bookmarks = bundle.bookmarks.map { it.toDomain() }
        validateBundle(bundle, descriptors, feedSources)

        val originalDescriptors = sourceCatalog.descriptors.value
        try {
            applySources(descriptors)
            val enabledSourceIds = sourceCatalog.descriptors.value
                .filter { it.enabled }
                .mapTo(mutableSetOf()) { it.id }
            val importedSettings = bundle.settings.filterKeys(USER_SETTING_KEYS::contains).toMutableMap()
            if (importedSettings["current_source_id"] !in enabledSourceIds) {
                importedSettings["current_source_id"] = enabledSourceIds.first()
            }
            withContext(ioDispatcher) {
                database.transaction {
                    for (feedSource in feedSources) upsertFeedSource(feedSource)
                    bookmarks.forEach { bookmark ->
                        database.bookmarkQueries.insert(bookmark.toEntity())
                        database.bookmarkTagQueries.deleteByBookmarkId(bookmark.id)
                        bookmark.tags.forEach { tag ->
                            database.tagQueries.insert(tag.toEntity())
                            database.bookmarkTagQueries.insert(BookmarkTag(bookmark.id, tag.id))
                        }
                    }
                    for (articleState in bundle.articleStates) mergeArticleState(articleState)
                    importedSettings.forEach { (key, value) ->
                        database.keyValueQueries.insertKeyValue(key, value)
                    }
                }
            }
        } catch (error: Throwable) {
            runCatching { restoreSources(originalDescriptors, descriptors) }
            throw error
        }

        UserDataImportReport(
            importedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            summary = bundle.summary(),
            sourceIds = descriptors.mapTo(mutableSetOf()) { it.id },
            feedSourceIds = feedSources.mapTo(mutableSetOf()) { it.id },
        )
    }

    override fun observeWebDavConfig(): Flow<WebDavConfig?> =
        database.keyValueQueries.getKeyValue(WEBDAV_CONFIG_KEY)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { row ->
                row?.content?.let { content ->
                    runCatching { json.decodeFromString<WebDavConfigSnapshot>(content).toDomain() }
                        .getOrNull()
                }
            }

    override suspend fun saveWebDavConfig(config: WebDavConfig?) = withContext(ioDispatcher) {
        if (config == null) {
            database.keyValueQueries.deleteKeyValue(WEBDAV_CONFIG_KEY)
        } else {
            database.keyValueQueries.insertKeyValue(
                WEBDAV_CONFIG_KEY,
                json.encodeToString(WebDavConfigSnapshot.fromDomain(config)),
            )
        }
        Unit
    }

    override suspend fun backupToWebDav(): Result<UserDataExport> {
        val config = currentWebDavConfig()
            ?: return Result.failure(IllegalStateException("请先配置 WebDAV"))
        return exportUserData().fold(
            onSuccess = { export -> webDav.write(config, export.payload).map { export } },
            onFailure = { Result.failure(it) },
        )
    }

    override suspend fun restoreFromWebDav(): Result<UserDataImportReport> {
        val config = currentWebDavConfig()
            ?: return Result.failure(IllegalStateException("请先配置 WebDAV"))
        return webDav.read(config).fold(
            onSuccess = { importUserData(it) },
            onFailure = { Result.failure(it) },
        )
    }

    private fun collectBundle(exportedAt: Long): UserDataBundle {
        val feedSources = database.feedSourceQueries.getAllFeedSources().executeAsList().map { row ->
            FeedSource(
                id = row.id,
                name = row.name,
                url = row.url,
                type = FeedType.valueOf(row.type),
                description = row.description,
                iconUrl = row.iconUrl,
                lastUpdate = row.lastUpdate,
                selectorConfig = row.selectorConfig?.let {
                    runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrDefault(emptyMap())
                }.orEmpty(),
                autoRefresh = row.autoRefresh == 1L,
                refreshInterval = row.refreshInterval,
            ).toSnapshot()
        }
        val bookmarks = database.bookmarkQueries.selectAll().executeAsList().map { row ->
            val tags = database.tagQueries.getTagsForBookmark(row.id).executeAsList().map { it.toDomain() }
            row.toDomain(tags).toSnapshot()
        }
        val articleStates = database.articleQueries.getAllArticleUserStates().executeAsList().map { row ->
            ArticleStateSnapshot(
                articleId = row.articleId,
                isRead = row.isRead != 0L,
                isBookmarked = row.isBookmarked != 0L,
                updatedAtEpochMillis = row.updatedAt,
            )
        }
        val settings = USER_SETTING_KEYS.mapNotNull { key ->
            database.keyValueQueries.getKeyValue(key).executeAsOneOrNull()?.content?.let { key to it }
        }.toMap()
        return UserDataBundle(
            exportedAtEpochMillis = exportedAt,
            sources = sourceCatalog.descriptors.value.map { it.toSnapshot() },
            feedSources = feedSources,
            bookmarks = bookmarks,
            articleStates = articleStates,
            settings = settings,
        )
    }

    private fun validateBundle(
        bundle: UserDataBundle,
        descriptors: List<ai.saniou.thread.domain.model.source.SourceDescriptor>,
        feeds: List<FeedSource>,
    ) {
        require(descriptors.distinctBy { it.id }.size == descriptors.size) { "Duplicate source id" }
        require(feeds.distinctBy { it.id }.size == feeds.size) { "Duplicate feed source id" }
        require(feeds.distinctBy { it.url }.size == feeds.size) { "Duplicate feed source URL" }
        require(bundle.bookmarks.distinctBy { it.id }.size == bundle.bookmarks.size) { "Duplicate bookmark id" }
        require(bundle.articleStates.distinctBy { it.articleId }.size == bundle.articleStates.size) {
            "Duplicate article state id"
        }
        val currentBuiltIns = sourceCatalog.descriptors.value.filter { it.isBuiltIn }.associateBy { it.id }
        descriptors.forEach { descriptor ->
            require(descriptor.id in currentBuiltIns || sourceCatalog.supports(descriptor.type)) {
                "No runtime factory for source '${descriptor.id}' (${descriptor.type})"
            }
        }
        require(descriptors.any { it.enabled } || sourceCatalog.descriptors.value.any { it.enabled }) {
            "At least one source must remain enabled"
        }
    }

    private suspend fun applySources(
        descriptors: List<ai.saniou.thread.domain.model.source.SourceDescriptor>,
    ) {
        val currentBuiltIns = sourceCatalog.descriptors.value.filter { it.isBuiltIn }.associateBy { it.id }
        val enabled = descriptors.filter { it.enabled }
        val disabled = descriptors.filterNot { it.enabled }
        (enabled + disabled).forEach { descriptor ->
            if (descriptor.id in currentBuiltIns) {
                sourceCatalog.setEnabled(descriptor.id, descriptor.enabled)
            } else {
                sourceCatalog.upsert(descriptor.copy(isBuiltIn = false))
            }
        }
    }

    private suspend fun restoreSources(
        original: List<ai.saniou.thread.domain.model.source.SourceDescriptor>,
        attempted: List<ai.saniou.thread.domain.model.source.SourceDescriptor>,
    ) {
        val originalIds = original.mapTo(mutableSetOf()) { it.id }
        attempted.filter { it.id !in originalIds }.forEach { descriptor ->
            if (sourceCatalog.descriptors.value.any { it.id == descriptor.id && !it.isBuiltIn }) {
                sourceCatalog.remove(descriptor.id)
            }
        }
        val enabled = original.filter { it.enabled }
        val disabled = original.filterNot { it.enabled }
        (enabled + disabled).forEach { descriptor ->
            if (descriptor.isBuiltIn) sourceCatalog.setEnabled(descriptor.id, descriptor.enabled)
            else sourceCatalog.upsert(descriptor)
        }
    }

    private suspend fun upsertFeedSource(source: FeedSource) {
        val existing = database.feedSourceQueries.getFeedSourceById(source.id).executeAsOneOrNull()
            ?: database.feedSourceQueries.getFeedSourceByUrl(source.url).executeAsOneOrNull()
        database.feedSourceQueries.insertFeedSource(
            id = existing?.id ?: source.id,
            name = source.name,
            url = source.url,
            type = source.type.name,
            description = source.description,
            iconUrl = source.iconUrl,
            lastUpdate = maxOf(existing?.lastUpdate ?: 0, source.lastUpdate),
            selectorConfig = json.encodeToString(source.selectorConfig),
            autoRefresh = if (source.autoRefresh) 1L else 0L,
            refreshInterval = source.refreshInterval,
        )
    }

    private suspend fun mergeArticleState(snapshot: ArticleStateSnapshot) {
        require(snapshot.articleId.isNotBlank()) { "Article state id must not be blank" }
        database.articleQueries.upsertArticleUserState(
            articleId = snapshot.articleId,
            isRead = if (snapshot.isRead) 1L else 0L,
            isBookmarked = if (snapshot.isBookmarked) 1L else 0L,
            updatedAt = snapshot.updatedAtEpochMillis,
        )
        val merged = database.articleQueries.getArticleUserState(snapshot.articleId).executeAsOne()
        database.articleQueries.applyArticleUserState(
            isRead = merged.isRead,
            isBookmarked = merged.isBookmarked,
            articleId = snapshot.articleId,
        )
    }

    private suspend fun currentWebDavConfig(): WebDavConfig? = withContext(ioDispatcher) {
        database.keyValueQueries.getKeyValue(WEBDAV_CONFIG_KEY).executeAsOneOrNull()?.content?.let {
            json.decodeFromString<WebDavConfigSnapshot>(it).toDomain()
        }
    }

    private fun UserDataBundle.summary() = SyncContentSummary(
        sourceCount = sources.size,
        feedSourceCount = feedSources.size,
        bookmarkCount = bookmarks.size,
        articleStateCount = articleStates.size,
        settingCount = settings.size,
    )

    private companion object {
        const val WEBDAV_CONFIG_KEY = "sync_webdav_config_v1"
        val USER_SETTING_KEYS = setOf(
            "current_source_id",
            "last_opened_forum_id",
            "last_opened_forum_source",
            "active_subscription_key",
        )
    }
}

@Serializable
private data class WebDavConfigSnapshot(
    val endpoint: String,
    val username: String,
    val password: String,
) {
    fun toDomain() = WebDavConfig(endpoint, username, password)

    companion object {
        fun fromDomain(value: WebDavConfig) =
            WebDavConfigSnapshot(value.endpoint, value.username, value.password)
    }
}
