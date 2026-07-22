package ai.saniou.thread.domain.usecase.block

import ai.saniou.thread.domain.repository.ContentBlockRepository

class RemoveContentBlockUseCase(
    private val repository: ContentBlockRepository,
) {
    suspend operator fun invoke(id: Long) = repository.removeBlock(id)
}
