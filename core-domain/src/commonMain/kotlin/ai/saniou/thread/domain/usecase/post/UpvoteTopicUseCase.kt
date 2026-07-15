package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.repository.ReactionRepository

class UpvoteTopicUseCase(private val repository: ReactionRepository) {
    suspend operator fun invoke(sourceId: String, topicId: String) =
        repository.upvoteTopic(sourceId, topicId)
}
