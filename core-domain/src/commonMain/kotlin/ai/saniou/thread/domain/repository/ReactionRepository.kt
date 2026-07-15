package ai.saniou.thread.domain.repository

interface ReactionRepository {
    suspend fun upvoteTopic(sourceId: String, topicId: String): Result<Unit>
}
