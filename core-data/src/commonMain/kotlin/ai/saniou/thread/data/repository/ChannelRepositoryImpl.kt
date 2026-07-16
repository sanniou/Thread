package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.cache.CacheFreshnessStore
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetTopicsInChannelKeyset
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.ChannelRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import ai.saniou.thread.domain.source.SourceCatalog
import ai.saniou.thread.domain.cache.CachePolicyProvider
import ai.saniou.thread.domain.cache.CacheResource
import androidx.paging.*
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ChannelRepositoryImpl(
    private val db: Database,
    private val sourceCatalog: SourceCatalog,
    private val cache: SourceCache,
    private val refreshCoordinator: RefreshCoordinator,
    private val cachePolicyProvider: CachePolicyProvider,
    private val freshnessStore: CacheFreshnessStore,
) : ChannelRepository {

    private val fallbackStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun getChannels(sourceId: String): Flow<List<Channel>> =
        sourceCatalog.source(sourceId)?.observeChannels() ?: flowOf(emptyList())

    override fun getRecentChannels(sourceId: String, limit: Long): Flow<List<Channel>> =
        db.channelQueries.getRecentChannels(sourceId, limit)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { channels -> channels.map { it.toDomain(db.channelQueries) } }

    override suspend fun fetchChannels(sourceId: String, forceRefresh: Boolean): Result<Unit> {
        val source = sourceCatalog.source(sourceId)
            ?: return Result.failure(IllegalArgumentException("Source not found: $sourceId"))
        val freshnessKey = CacheFreshnessStore.channelCatalog(sourceId)
        val policy = cachePolicyProvider.policy(sourceId, CacheResource.CHANNEL_CATALOG)
        val hasCachedChannels = withContext(ioDispatcher) { cache.getChannels(sourceId).isNotEmpty() }
        if (!forceRefresh && freshnessStore.isFresh(freshnessKey, policy) && hasCachedChannels) {
            return Result.success(Unit)
        }
        return refreshCoordinator.execute(
            key = "forum:$sourceId:catalog",
            label = "${source.name} 版块",
            operation = source::fetchChannels,
        ).onSuccess { freshnessStore.markFresh(freshnessKey) }
    }

    override fun getChannelTopicsPaging(
        sourceId: String,
        channelId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Topic>> {
        val source = sourceCatalog.source(sourceId) ?: return flowOf(PagingData.empty())
        val pageSize = 20

        return Pager(
            config = threadPagingConfig(pageSize),
            initialKey = initialPage,
            remoteMediator = GenericRemoteMediator(
                db = db,
                dataPolicy = DataPolicy.NETWORK_ELSE_CACHE,
                remoteKeyStrategy = DefaultRemoteKeyStrategy(
                    db = db,
                    type = "channel_${sourceId}_${channelId}_${isTimeline}",
                    itemTargetIdExtractor = { item -> item.id }
                ),
                fetcher = { cursor ->
                    // Pass the cursor string to the source
                    source.getChannelTopics(channelId, cursor, isTimeline)
                },
                saver = { topics, loadType, cursor, receiveDate, startOrder ->
                    if (loadType == LoadType.REFRESH) {
                        cache.clearChannelCache(sourceId, channelId)
                    }
                    cache.saveTopics(
                        topics = topics,
                        sourceId = sourceId,
                        channelId = channelId,
                        receiveDate = receiveDate,
                        startOrder = startOrder,
                    )
                },
                itemTargetIdExtractor = { item -> item.id },
                lastItemMetadataExtractor = { item ->
                    item.receiveDate to item.receiveOrder
                },
            ),
            pagingSourceFactory = {
                // SQLDelight's official QueryPagingSource is provided by the source cache.
                val key = "${sourceId}_${channelId}"
                val isFallback = fallbackStates.value[key] == true
                cache.getChannelTopicPagingSource(sourceId, channelId, isFallback)
            }
        ).flow.map { pagingData ->
            pagingData.map { item ->
                item.toDomain(db.commentQueries, db.imageQueries).copy(
                    sourceUrl = source.topicUrl(item.id),
                )
            }
        }
    }

    override fun getChannelName(sourceId: String, channelId: String): Flow<String?> {
        val source = sourceCatalog.source(sourceId) ?: return flowOf(null)
        return source.getChannel(channelId).map { it?.name }
    }

    override fun getChannelDetail(sourceId: String, channelId: String): Flow<Channel?> {
        val source = sourceCatalog.source(sourceId) ?: return flowOf(null)
        return source.getChannel(channelId)
    }

    override suspend fun saveLastOpenedChannel(channel: Channel?) {
        withContext(ioDispatcher) {
            if (channel != null) {
                db.keyValueQueries.insertKeyValue("last_opened_forum_id", channel.id)
                db.keyValueQueries.insertKeyValue("last_opened_forum_source", channel.sourceId)
                db.channelQueries.upsertChannelVisit(
                    sourceId = channel.sourceId,
                    channelId = channel.id,
                    visitedAt = Clock.System.now().toEpochMilliseconds(),
                )
            } else {
                db.keyValueQueries.deleteKeyValue("last_opened_forum_id")
                db.keyValueQueries.deleteKeyValue("last_opened_forum_source")
            }
        }
    }

    override suspend fun getLastOpenedChannel(): Channel? {
        return withContext(ioDispatcher) {
            val fid =
                db.keyValueQueries.getKeyValue("last_opened_forum_id").executeAsOneOrNull()?.content
            val sourceId =
                db.keyValueQueries.getKeyValue("last_opened_forum_source")
                    .executeAsOneOrNull()?.content
                    ?: "nmb"

            if (fid != null) {
                // 如果是 NMB，尝试从数据库获取完整信息
                // 如果是其他源，可能需要通过 source.getForum 获取，或者仅仅返回一个只有 ID 的 Forum 对象
                // 这里为了简单，如果数据库有就从数据库取（适用于 NMB），否则构造一个简单的对象
                val source = sourceCatalog.source(sourceId)
                // 尝试从 Source 获取
                if (source != null) {
                    try {
                        source.getChannel(fid).firstOrNull()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override suspend fun setFallbackMode(sourceId: String, channelId: String, enabled: Boolean) {
        val key = "${sourceId}_${channelId}"
        val current = fallbackStates.value
        if (current[key] != enabled) {
            fallbackStates.value = current + (key to enabled)
            // Notify SQLDelight so the active PagingSource is recreated with the new fallback
            // policy. The no-op table update preserves domain data and history ordering.
            withContext(ioDispatcher) {
                db.topicQueries.touchChannelTopics(sourceId, channelId)
            }
        }
    }
}
