package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.activity.SourceIdentityStatus
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    fun observe(): Flow<List<SourceIdentityStatus>>

    suspend fun markAuthenticated(sourceId: String, validatedAtEpochMillis: Long)

    suspend fun markExpired(sourceId: String, message: String?, detectedAtEpochMillis: Long)

    suspend fun clear(sourceId: String)
}
