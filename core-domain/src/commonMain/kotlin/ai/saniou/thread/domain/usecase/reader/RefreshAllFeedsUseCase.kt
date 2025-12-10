package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.repository.ReaderRepository

class RefreshAllFeedsUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke() = repository.refreshAllFeeds()
}