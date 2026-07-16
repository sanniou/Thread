package ai.saniou.thread.domain.usecase.operations

import ai.saniou.thread.domain.repository.OperationsRepository

class ObserveOperationsUseCase(private val repository: OperationsRepository) {
    operator fun invoke() = repository.observe()
}

class ClearSourceDiagnosticUseCase(private val repository: OperationsRepository) {
    operator fun invoke(sourceId: String) = repository.clearRefreshDiagnostic(sourceId)
}
