package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.repository.PostRepository
import ai.saniou.thread.domain.source.ConnectorRegistry

class PostRepositoryImpl(
    private val registry: ConnectorRegistry,
) : PostRepository {
    override suspend fun createThread(
        sourceId: String,
        channelId: String,
        draft: PostDraft,
    ): PostResult = registry.requirePosting(sourceId).createThread(channelId, draft)

    override suspend fun createReply(
        sourceId: String,
        topicId: String,
        draft: PostDraft,
    ): PostResult = registry.requirePosting(sourceId).createReply(topicId, draft)
}

private fun ConnectorRegistry.requirePosting(sourceId: String) = posting(sourceId)
    ?: throw UnsupportedOperationException("Source '$sourceId' does not support posting")
