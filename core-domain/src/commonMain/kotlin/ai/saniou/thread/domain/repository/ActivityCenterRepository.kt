package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.activity.ActivityCenterSnapshot
import kotlinx.coroutines.flow.Flow

interface ActivityCenterRepository {
    fun observe(): Flow<ActivityCenterSnapshot>
    suspend fun clearCompletedActions()
}
