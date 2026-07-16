package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.operations.OperationsSnapshot
import kotlinx.coroutines.flow.Flow

interface OperationsRepository {
    fun observe(): Flow<OperationsSnapshot>
    fun clearRefreshDiagnostic(sourceId: String)
}
