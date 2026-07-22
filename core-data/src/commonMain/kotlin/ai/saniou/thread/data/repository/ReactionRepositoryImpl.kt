package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.cache.CacheFreshnessStore
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.repository.ReactionRepository
import ai.saniou.thread.domain.source.ConnectorRegistry
import kotlinx.coroutines.withContext

class ReactionRepositoryImpl(
    private val registry: ConnectorRegistry,
    private val database: Database,
    private val freshnessStore: CacheFreshnessStore,
) : ReactionRepository {
    override suspend fun upvoteTopic(sourceId: String, topicId: String): Result<Unit> =
        react(sourceId, topicId) { connector, postId -> connector.upvote(topicId, postId) }

    override suspend fun downvoteTopic(sourceId: String, topicId: String): Result<Unit> =
        react(sourceId, topicId) { connector, postId -> connector.downvote(topicId, postId) }

    private suspend fun react(
        sourceId: String,
        topicId: String,
        action: suspend (ai.saniou.thread.domain.source.ReactionConnector, String) -> Result<Unit>,
    ): Result<Unit> {
        val connector = registry.reactions(sourceId)
            ?: return Result.failure(UnsupportedOperationException("Source '$sourceId' does not support reactions"))
        val rootPost = withContext(ioDispatcher) {
            database.commentQueries.getAllCommentsByTopicId(sourceId, topicId)
                .executeAsList()
                .minByOrNull { it.floor }
        }
            ?: return Result.failure(IllegalStateException("请先加载主题回复，以确定主贴 ID"))
        return action(connector, rootPost.id).onSuccess {
            freshnessStore.invalidate(CacheFreshnessStore.topic(sourceId, topicId))
        }
    }
}
