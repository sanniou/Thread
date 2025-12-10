package ai.saniou.thread.domain.repository

import kotlin.reflect.KType

interface SettingsRepository {
    suspend fun <T : Any> saveValue(key: String, value: T?, type: KType)
    suspend fun <T : Any> getValue(key: String, type: KType): T?
}

suspend inline fun <reified T : Any> SettingsRepository.saveValue(key: String, value: T?) {
    saveValue(key, value, kotlin.reflect.typeOf<T>())
}

suspend inline fun <reified T : Any> SettingsRepository.getValue(key: String): T? {
    return getValue(key, kotlin.reflect.typeOf<T>())
}
