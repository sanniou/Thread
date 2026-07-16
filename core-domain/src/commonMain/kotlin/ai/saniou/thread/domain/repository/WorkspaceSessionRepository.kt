package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.workspace.WorkspaceSession
import kotlinx.coroutines.flow.Flow

interface WorkspaceSessionRepository {
    fun observe(): Flow<WorkspaceSession>
    suspend fun get(): WorkspaceSession
    suspend fun save(session: WorkspaceSession)
    suspend fun update(transform: (WorkspaceSession) -> WorkspaceSession)
}
