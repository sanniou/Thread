package ai.saniou.thread.domain.usecase.workspace

import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import ai.saniou.thread.domain.repository.WorkspaceSessionRepository

class ObserveWorkspaceSessionUseCase(private val repository: WorkspaceSessionRepository) {
    operator fun invoke() = repository.observe()
}

class SaveWorkspaceSessionUseCase(private val repository: WorkspaceSessionRepository) {
    suspend operator fun invoke(session: WorkspaceSession) = repository.save(session)
}

class UpdateWorkspaceSessionUseCase(private val repository: WorkspaceSessionRepository) {
    suspend operator fun invoke(transform: (WorkspaceSession) -> WorkspaceSession) = repository.update(transform)
}
