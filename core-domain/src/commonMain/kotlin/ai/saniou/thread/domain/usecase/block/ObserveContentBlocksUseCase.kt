package ai.saniou.thread.domain.usecase.block

import ai.saniou.thread.domain.model.block.ContentBlock
import ai.saniou.thread.domain.repository.ContentBlockRepository
import kotlinx.coroutines.flow.Flow

class ObserveContentBlocksUseCase(
    private val repository: ContentBlockRepository,
) {
    operator fun invoke(): Flow<List<ContentBlock>> = repository.observeBlocks()
}
