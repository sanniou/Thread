package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.settings.AppearancePreferences
import ai.saniou.thread.domain.repository.AppearanceRepository
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.domain.repository.saveValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AppearanceRepositoryImpl(
    private val settings: SettingsRepository,
) : AppearanceRepository {
    override fun observe(): Flow<AppearancePreferences> =
        settings.observeValue<AppearancePreferences>(AppearancePreferences.SETTINGS_KEY)
            .map { it ?: AppearancePreferences() }
            .catch { emit(AppearancePreferences()) }

    override suspend fun save(preferences: AppearancePreferences) {
        settings.saveValue(AppearancePreferences.SETTINGS_KEY, preferences)
    }

    override suspend fun reset() {
        settings.saveValue<AppearancePreferences>(AppearancePreferences.SETTINGS_KEY, null)
    }
}
