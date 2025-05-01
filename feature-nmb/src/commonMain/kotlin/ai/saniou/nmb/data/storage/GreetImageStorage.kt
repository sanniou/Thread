package ai.saniou.nmb.data.storage

import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageStorage
import io.github.irgaly.kottage.getOrNull
import io.github.irgaly.kottage.platform.KottageContext
import io.github.irgaly.kottage.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours

/**
 * 管理欢迎图片的持久化存储
 */
class GreetImageStorage(
    private val scope: CoroutineScope
) {
    private val directoryPath by lazy { getStorageDirectory() }
    private val kottage: Kottage by lazy {
        Kottage(
            name = "nmb-greet-image-storage",
            directoryPath = directoryPath,
            environment = KottageEnvironment(),
            scope = scope
        )
    }

    private val storage: KottageStorage by lazy {
        kottage.storage("greet-image")
    }

    private val _greetImageUrl = MutableStateFlow<String?>(null)
    val greetImageUrl = _greetImageUrl.asStateFlow()

    /**
     * 检查缓存的欢迎图片是否过期（超过3小时）
     */
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
    suspend fun saveGreetImageUrl(url: String) {
        storage.put("greet_image_url", url)
        storage.put(
            "cache_info", GreetImageCacheInfo(
                lastUpdateTime = Clock.System.now().toEpochMilliseconds()
            )
        )
        _greetImageUrl.value = url
    }

    /**
     * 关闭存储
     */
    suspend fun close() {
        kottage.close()
    }

    @Serializable
    private data class GreetImageCacheInfo(
        val lastUpdateTime: Long
    )

    /**
     * 创建 KottageEnvironment
     */
    private fun KottageEnvironment(): io.github.irgaly.kottage.KottageEnvironment {
        return io.github.irgaly.kottage.KottageEnvironment(
            context = KottageContext()
        )
    }
}
