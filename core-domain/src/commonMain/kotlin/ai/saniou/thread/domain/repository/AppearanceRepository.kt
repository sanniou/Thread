package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.settings.AppearancePreferences
import kotlinx.coroutines.flow.Flow

interface AppearanceRepository {
    fun observe(): Flow<AppearancePreferences>
    suspend fun save(preferences: AppearancePreferences)
    suspend fun reset()
}
