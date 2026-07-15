package ai.saniou.thread.domain.usecase.refresh

import ai.saniou.thread.domain.refresh.RefreshCoordinator

class ObserveRefreshDiagnosticsUseCase(
    private val coordinator: RefreshCoordinator,
) {
    operator fun invoke() = coordinator.states
}
