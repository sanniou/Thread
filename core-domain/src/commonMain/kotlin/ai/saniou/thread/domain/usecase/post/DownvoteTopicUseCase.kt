package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.repository.ReactionRepository

class DownvoteTopicUseCase(private val repository: ReactionRepository) {
    suspend operator fun invoke(sourceId: String, topicId: String) =
        repository.downvoteTopic(sourceId, topicId)
}
