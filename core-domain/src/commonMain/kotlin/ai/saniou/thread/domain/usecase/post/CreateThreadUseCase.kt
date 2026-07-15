package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.repository.PostRepository

class CreateThreadUseCase(private val postRepository: PostRepository) {
    suspend operator fun invoke(
        fid: Int,
        draft: PostDraft,
    ): String {
        return postRepository.post(fid, draft)
    }
}
