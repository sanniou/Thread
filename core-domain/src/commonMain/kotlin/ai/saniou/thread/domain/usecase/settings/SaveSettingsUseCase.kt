package ai.saniou.thread.domain.usecase.settings

import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.saveValue

class SaveSettingsUseCase(val settingsRepository: SettingsRepository) {
    suspend inline operator fun <reified T : Any> invoke(key: String, value: T?) {
        settingsRepository.saveValue(key, value)
    }
}
