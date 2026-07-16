package ai.saniou.thread.domain.usecase.workspace

import ai.saniou.thread.domain.model.workspace.RestorableContentReference
import ai.saniou.thread.domain.repository.WorkspaceRestorationRepository

class ValidateRestorableContentUseCase(private val repository: WorkspaceRestorationRepository) {
    suspend operator fun invoke(reference: RestorableContentReference) = repository.isAvailable(reference)
}
