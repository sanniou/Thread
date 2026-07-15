package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.repository.PostRepository
import ai.saniou.thread.domain.repository.PostResult

class CreateReplyUseCase(private val postRepository: PostRepository) {
    suspend operator fun invoke(
        sourceId: String,
        topicId: String,
        draft: PostDraft,
    ): PostResult {
        return postRepository.createReply(sourceId, topicId, draft)
    }
}
