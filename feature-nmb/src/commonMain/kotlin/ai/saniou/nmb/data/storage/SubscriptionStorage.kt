package ai.saniou.nmb.data.storage

import io.github.irgaly.kottage.KottageList
import io.github.irgaly.kottage.add
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
class SubscriptionStorage(scope: CoroutineScope) : BasicStorage(scope, "subscription") {

    private val subscriptionList: KottageList by lazy {
        storage.list("subscription_lis")
    }

    private val _subscriptionId = MutableStateFlow<String?>(null)
    val subscriptionId = _subscriptionId.asStateFlow()


    /**
     * 获取订阅ID
     * 如果不存在则返回null
     */
    suspend fun getLastSubscriptionId(): String? {
        val id = subscriptionList.getLast()?.value<SubscriptionInfo>()?.id
        _subscriptionId.value = id
        return id
    }

    suspend fun getSubscriptionId(): List<String> {
        return subscriptionList.getPageFrom(null, null).items.map {
            it.value<SubscriptionInfo>().id
        }
    }

    /**
     * 保存订阅ID
     */
    suspend fun addSubscriptionId(id: String) {
        subscriptionList.add(
            "subscription_info",
            SubscriptionInfo(id, lastUpdateTime = Clock.System.now().toEpochMilliseconds())
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

    @Serializable
    private data class SubscriptionInfo(
        val id: String,
        val lastUpdateTime: Long
    )
}
