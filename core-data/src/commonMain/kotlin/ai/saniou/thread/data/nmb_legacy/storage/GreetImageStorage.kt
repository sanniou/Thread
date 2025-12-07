package ai.saniou.nmb.data.storage

import io.github.irgaly.kottage.getOrNull
import io.github.irgaly.kottage.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * 管理欢迎图片的持久化存储
 */
class GreetImageStorage(scope: CoroutineScope) : BasicStorage(scope, "greet-image") {

    private val _greetImageUrl = MutableStateFlow<String?>(null)
    val greetImageUrl = _greetImageUrl.asStateFlow()

    /**
     * 检查缓存的欢迎图片是否过期（超过3小时）
     */
    @OptIn(ExperimentalTime::class)
    suspend fun isGreetImageExpired(): Boolean {
        val lastUpdateTime = storage.getOrNull<GreetImageCacheInfo>("cache_info")?.lastUpdateTime
        if (lastUpdateTime == null) return true

        val now = Clock.System.now()
        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastUpdateTime)
        return now - lastUpdateInstant > 3.hours
    }

    /**
     * 获取缓存的欢迎图片URL
     */
    suspend fun getCachedGreetImageUrl(): String? {
        val url = storage.getOrNull<String>("greet_image_url")
        _greetImageUrl.value = url
        return url
    }

    /**
     * 保存欢迎图片URL到缓存
     */
    @OptIn(ExperimentalTime::class)
    suspend fun saveGreetImageUrl(url: String) {
        storage.put("greet_image_url", url)
        storage.put(
            "cache_info", GreetImageCacheInfo(
                lastUpdateTime = Clock.System.now().toEpochMilliseconds()
            )
        )
        _greetImageUrl.value = url
    }

    @Serializable
    private data class GreetImageCacheInfo(
        val lastUpdateTime: Long
    )
}
