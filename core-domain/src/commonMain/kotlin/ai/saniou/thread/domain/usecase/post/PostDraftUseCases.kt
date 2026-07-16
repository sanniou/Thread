package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.model.forum.PostDraftKey
import ai.saniou.thread.domain.model.forum.SavedPostDraft
import ai.saniou.thread.domain.repository.PostDraftRepository

class ObservePostDraftsUseCase(private val repository: PostDraftRepository) {
    operator fun invoke() = repository.observeAll()
}

class GetPostDraftUseCase(private val repository: PostDraftRepository) {
    suspend operator fun invoke(key: PostDraftKey) = repository.get(key)
}

class SavePostDraftUseCase(private val repository: PostDraftRepository) {
    suspend operator fun invoke(draft: SavedPostDraft) = repository.save(draft)
}

class DiscardPostDraftUseCase(private val repository: PostDraftRepository) {
    suspend operator fun invoke(key: PostDraftKey) = repository.discard(key)
}
