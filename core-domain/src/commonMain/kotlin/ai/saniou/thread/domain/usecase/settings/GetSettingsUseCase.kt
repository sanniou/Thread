package ai.saniou.thread.domain.usecase.settings

import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue

class GetSettingsUseCase(val settingsRepository: SettingsRepository) {
    suspend inline operator fun <reified T : Any> invoke(key: String): T? {
        return settingsRepository.getValue(key)
    }
}
