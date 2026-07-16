package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.workspace.RestorableContentReference

interface WorkspaceRestorationRepository {
    /** Validation is cache-only and must never trigger a connector request. */
    suspend fun isAvailable(reference: RestorableContentReference): Boolean
}
