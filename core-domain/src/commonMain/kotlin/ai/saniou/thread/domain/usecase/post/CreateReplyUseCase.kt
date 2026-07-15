package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.repository.PostRepository

class CreateReplyUseCase(private val postRepository: PostRepository) {
    suspend operator fun invoke(
        resto: Int,
        draft: PostDraft,
    ): String {
        return postRepository.reply(resto, draft)
    }
}
