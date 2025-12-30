package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.repository.ReferenceRepository
import kotlinx.coroutines.flow.Flow

class GetReferenceUseCase(
    private val referenceRepository: ReferenceRepository
) {
    operator fun invoke(id: Long): Flow<Comment> {
        return referenceRepository.getReference(id)
    }
}
