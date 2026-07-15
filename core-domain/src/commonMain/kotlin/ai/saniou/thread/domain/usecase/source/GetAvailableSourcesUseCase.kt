package ai.saniou.thread.domain.usecase.source

import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow

class GetAvailableSourcesUseCase(
    private val sourceRepository: SourceRepository
) {
    operator fun invoke(): Flow<List<Source>> = sourceRepository.observeAvailableSources()

    fun current(): List<Source> = sourceRepository.getAvailableSources()
}
