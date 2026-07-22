package ai.saniou.thread.domain.repository

interface ReactionRepository {
    suspend fun upvoteTopic(sourceId: String, topicId: String): Result<Unit>
    suspend fun downvoteTopic(sourceId: String, topicId: String): Result<Unit>
}
