package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.model.reader.ReaderSubscriptionFormat
import ai.saniou.thread.domain.reader.ReaderRefreshScheduler
import ai.saniou.thread.domain.repository.ReaderRepository

class ExportReaderSubscriptionsUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(format: ReaderSubscriptionFormat) =
        repository.exportSubscriptions(format)
}

class ImportReaderSubscriptionsUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(payload: String, format: ReaderSubscriptionFormat) =
        repository.importSubscriptions(payload, format)
}

class ObserveReaderSchedulerUseCase(private val scheduler: ReaderRefreshScheduler) {
    operator fun invoke() = scheduler.state
}

class RefreshDueReaderFeedsUseCase(private val scheduler: ReaderRefreshScheduler) {
    suspend operator fun invoke() = scheduler.refreshDueNow()
}
