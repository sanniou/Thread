package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import ai.saniou.thread.domain.model.operations.DiagnosticExport
import kotlinx.coroutines.flow.Flow

interface OperationsRepository {
    fun observe(): Flow<OperationsSnapshot>
    suspend fun clearRefreshDiagnostic(sourceId: String)
    suspend fun exportDiagnostic(): DiagnosticExport
}
