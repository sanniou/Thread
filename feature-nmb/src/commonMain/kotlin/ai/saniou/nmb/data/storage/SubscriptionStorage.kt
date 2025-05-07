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
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 管理订阅相关数据的持久化存储
 */
class SubscriptionStorage(
    private val scope: CoroutineScope
) {
    private val directoryPath by lazy { getStorageDirectory() }
    private val kottage: Kottage by lazy {
        Kottage(
            name = "nmb-subscription-storage",
            directoryPath = directoryPath,
            environment = KottageEnvironment(),
            scope = scope
        )
    }

    private val storage: KottageStorage by lazy {
        kottage.storage("subscription")
    }

    private val _subscriptionId = MutableStateFlow<String?>(null)
    val subscriptionId = _subscriptionId.asStateFlow()

    /**
     * 获取订阅ID
     * 如果不存在则返回null
     */
    suspend fun getSubscriptionId(): String? {
        val id = storage.getOrNull<String>("subscription_id")
        _subscriptionId.value = id
        return id
    }

    /**
     * 保存订阅ID
     */
    suspend fun saveSubscriptionId(id: String) {
        storage.put("subscription_id", id)
        storage.put(
            "subscription_info", SubscriptionInfo(
                lastUpdateTime = Clock.System.now().toEpochMilliseconds()
            )
        )
        _subscriptionId.value = id
    }

    /**
     * 生成随机订阅ID
     */
    @OptIn(ExperimentalUuidApi::class)
    fun generateRandomSubscriptionId(): String {
        return Uuid.random().toString()
    }

    /**
     * 关闭存储
     */
    suspend fun close() {
        kottage.close()
    }

    @Serializable
    private data class SubscriptionInfo(
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
