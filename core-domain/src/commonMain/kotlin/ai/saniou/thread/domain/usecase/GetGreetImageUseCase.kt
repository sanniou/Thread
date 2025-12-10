package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.getValue
import ai.saniou.thread.domain.repository.saveValue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class GetGreetImageUseCase(private val settingsRepository: SettingsRepository) {

    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): String {
        val cachedUrl = settingsRepository.getValue<String>(KEY_GREET_IMAGE_URL)
        val lastFetchTime = settingsRepository.getValue<Long>(KEY_LAST_FETCH_TIME) ?: 0
        val now = Clock.System.now()
        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastFetchTime)
        val isExpired = now - lastUpdateInstant > 3.hours

        return if (cachedUrl != null && !isExpired) {
            cachedUrl
        } else {
            val newUrl = getGreetImageUrlWithTimestamp()
            settingsRepository.saveValue(KEY_GREET_IMAGE_URL, newUrl)
            settingsRepository.saveValue(KEY_LAST_FETCH_TIME, now.toEpochMilliseconds())
            newUrl
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getGreetImageUrlWithTimestamp(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "https://nmb.ovear.info/h.php?time=$timestamp"
    }

    companion object {
        private const val KEY_GREET_IMAGE_URL = "key_greet_image_url"
        private const val KEY_LAST_FETCH_TIME = "key_last_fetch_time_greet_image"
    }
}
