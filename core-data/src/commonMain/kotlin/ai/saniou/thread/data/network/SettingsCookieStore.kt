package ai.saniou.thread.data.network

import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import ai.saniou.thread.network.cookie.CookieStore

/**
 * 基于 SettingsRepository 的持久化 Cookie 存储
 */
class SettingsCookieStore(
    private val settingsRepository: SettingsRepository
) : CookieStore {

    override suspend fun getCookie(sourceId: String): String? {
        return settingsRepository.getValue(getSettingsKey(sourceId))
    }

    override suspend fun saveCookie(sourceId: String, cookie: String) {
        settingsRepository.saveValue(getSettingsKey(sourceId), cookie)
    }

    override suspend fun clearCookie(sourceId: String) {
        settingsRepository.saveValue(getSettingsKey(sourceId), null as String?)
    }

    private fun getSettingsKey(sourceId: String): String {
        return "cookie_store_${sourceId}"
    }
}