package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.model.ThreadReply
import ai.saniou.thread.domain.repository.ReferenceRepository
import kotlinx.coroutines.flow.Flow

class GetReferenceUseCase(
    private val referenceRepository: ReferenceRepository
) {
    operator fun invoke(id: Long): Flow<ThreadReply> {
        return referenceRepository.getReference(id)
    }
}
