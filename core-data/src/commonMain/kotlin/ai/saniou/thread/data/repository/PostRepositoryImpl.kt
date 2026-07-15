package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.cache.CacheFreshnessStore
import ai.saniou.thread.data.cache.SourceCache
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.repository.PostRepository
import ai.saniou.thread.domain.source.ConnectorRegistry
import kotlinx.coroutines.withContext

class PostRepositoryImpl(
    private val registry: ConnectorRegistry,
    private val cache: SourceCache,
    private val freshnessStore: CacheFreshnessStore,
) : PostRepository {
    override suspend fun createThread(
        sourceId: String,
        channelId: String,
        draft: PostDraft,
    ): PostResult {
        val result = registry.requirePosting(sourceId).createThread(channelId, draft)
        withContext(ioDispatcher) { cache.clearChannelCache(sourceId, channelId) }
        freshnessStore.invalidate(CacheFreshnessStore.channelCatalog(sourceId))
        return result
    }

    override suspend fun createReply(
        sourceId: String,
        topicId: String,
        draft: PostDraft,
    ): PostResult {
        val result = registry.requirePosting(sourceId).createReply(topicId, draft)
        withContext(ioDispatcher) { cache.clearTopicCommentsCache(sourceId, topicId) }
        freshnessStore.invalidate(CacheFreshnessStore.comments(sourceId, topicId))
        freshnessStore.invalidate(CacheFreshnessStore.topic(sourceId, topicId))
        return result
    }
}

private fun ConnectorRegistry.requirePosting(sourceId: String) = posting(sourceId)
    ?: throw UnsupportedOperationException("Source '$sourceId' does not support posting")
