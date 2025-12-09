package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.ThreadReply
import ai.saniou.thread.domain.repository.ReferenceRepository

class GetReferenceUseCase(
    private val referenceRepository: ReferenceRepository,
) {
    suspend operator fun invoke(id: Long): Result<ThreadReply> {
        return referenceRepository.getReference(id)
    }
}
