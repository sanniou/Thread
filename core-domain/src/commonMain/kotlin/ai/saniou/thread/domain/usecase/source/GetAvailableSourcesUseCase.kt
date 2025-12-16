package ai.saniou.thread.domain.usecase.source

import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceRepository

class GetAvailableSourcesUseCase(
    private val sourceRepository: SourceRepository
) {
    operator fun invoke(): List<Source> {
        return sourceRepository.getAvailableSources()
    }
}