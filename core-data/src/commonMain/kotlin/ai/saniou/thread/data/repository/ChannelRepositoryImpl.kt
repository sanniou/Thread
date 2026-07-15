package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.DefaultRemoteKeyStrategy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.GetTopicsInChannelKeyset
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.ChannelRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import androidx.paging.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val db: Database,
    private val sources: Set<Source>,
    private val cache: SourceCache,
    private val refreshCoordinator: RefreshCoordinator,
) : ChannelRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }
    private val fallbackStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun getChannels(sourceId: String): Flow<List<Channel>> =
        sourceMap[sourceId]?.observeChannels() ?: flowOf(emptyList())
    override suspend fun fetchChannels(sourceId: String): Result<Unit> {
        val source = sourceMap[sourceId]
            ?: return Result.failure(IllegalArgumentException("Source not found: $sourceId"))
        return refreshCoordinator.execute(
            key = "forum:$sourceId:catalog",
            label = "${source.name} 版块",
            operation = source::fetchChannels,
        )
    }

    override fun getChannelTopicsPaging(
        sourceId: String,
        channelId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Topic>> {
        val source = sourceMap[sourceId] ?: return flowOf(PagingData.empty())
        val pageSize = 20

        return Pager(
            config = PagingConfig(pageSize = pageSize),
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
                // Use the new KeysetPagingSource from Cache
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
        val source = sourceMap[sourceId] ?: return flowOf(null)
        return source.getChannel(channelId).map { it?.name }
    }

    override fun getChannelDetail(sourceId: String, channelId: String): Flow<Channel?> {
        val source = sourceMap[sourceId] ?: return flowOf(null)
        return source.getChannel(channelId)
    }

    override suspend fun saveLastOpenedChannel(channel: Channel?) {
        withContext(ioDispatcher) {
            if (channel != null) {
                db.keyValueQueries.insertKeyValue("last_opened_forum_id", channel.id)
                db.keyValueQueries.insertKeyValue("last_opened_forum_source", channel.sourceName)
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
                val source = sourceMap[sourceId]
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
            // Trigger DB notification to invalidate PagingSource
            // Updating an unrelated table or a dummy field is enough.
            // Here we use a safe side-effect: update lastVisitedAt of a non-existent topic or similar trick.
            // Or better: Use the invalidationTracker if exposed. But we are in Repo.
            // Simple hack: Update a dummy KeyValue or just let the PagingSource factory pick it up next time?
            // No, we MUST invalidate the current PagingSource to force factory re-creation.
            // Writing to `Topic` table is the surest way.
            // We can update `receiveDate` of a dummy row, but that's messy.
            // Actually, SqlDelight PagingSource listens to the table.
            // Let's update `lastVisitedAt` of a dummy topic for this channel.
            // Or update `Channel` table? PagingSource queries `Topic`, does it listen to `Channel`? No.
            // It listens to `Topic`.
            // So we must touch `Topic`.
            // Let's execute a no-op update on Topic table for this channel.
            // "UPDATE Topic SET receiveDate = receiveDate WHERE sourceId = ... AND id = 'DUMMY_TRIGGER'"
            // But we don't have a dummy row.
            // Update a row that definitely exists?
            // How about: UPDATE Topic SET receiveDate = receiveDate WHERE sourceId = :sid AND channelId = :cid LIMIT 1;
            // Sqldelight triggers on Table change, not specific row.
            // So any update to Topic table works.
            withContext(ioDispatcher) {
                // This query doesn't change data but triggers notification
                // We need to define a 'touch' query in .sq or reuse an existing one with same values.
                // Re-setting lastVisitedAt for any topic in this channel?
                // Let's add a `touchTopics` query in Topic.sq later.
                // For now, let's assume we can use `updateTopicLastAccessTime` with current time? No side effect?
                // No, that changes sort order of history.
                // Let's add `touchChannelTopics` to Topic.sq.
                // For now, I will add a TODO and create the query.
                db.topicQueries.touchChannelTopics(sourceId, channelId)
            }
        }
    }
}
