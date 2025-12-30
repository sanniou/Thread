package ai.saniou.thread.domain.repository

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ai.saniou.thread.domain.model.forum.Topic

/**
 * 订阅仓库接口，定义了订阅功能的标准契约。
 */
interface SubscriptionRepository {

    /**
     * 获取当前激活的订阅Key。
     */
    suspend fun getActiveSubscriptionKey(): String?

    /**
     * 观察当前激活的订阅Key。
     */
    fun observeActiveSubscriptionKey(): Flow<String?>

    /**
     * 获取指定订阅ID的帖子流。
     *
     * @param subscriptionKey 订阅的唯一标识。
     * @return 一个包含帖子分页数据的 Flow。
     */
    fun getSubscriptionFeed(subscriptionKey: String): Flow<PagingData<Topic>>

    /**
     * 切换帖子的订阅状态。
     *
     * @param subscriptionKey 订阅的唯一标识。
     * @param post 需要切换状态的帖子。
     * @param isSubscribed 当前是否已订阅。
     * @return 操作结果。
     */
    suspend fun toggleSubscription(
        subscriptionKey: String,
        subscriptionId: String,
        isSubscribed: Boolean,
    ): Result<String>

    /**
     * 检查指定帖子是否已被订阅。
     *
     * @param subscriptionKey 订阅的唯一标识。
     * @param postId 帖子ID。
     * @return 一个布尔值的 Flow，true 表示已订阅。
     */
    fun isSubscribed(subscriptionKey: String, postId: String): Flow<Boolean>

    /**
     * 同步本地的订阅到远程。
     *
     * @param subscriptionKey 订阅的唯一标识。
     */
    suspend fun syncLocalSubscriptions(subscriptionKey: String)

    /**
     * 检查是否存在本地订阅。
     *
     * @param subscriptionKey 订阅的唯一标识。
     * @return 如果存在本地订阅，则返回 true。
     */
    suspend fun hasLocalSubscriptions(subscriptionKey: String): Boolean

    /**
     * 获取所有订阅Key。
     * @return 一个包含所有订阅Key的Flow。
     */
    fun getSubscriptionKeys(): Flow<List<String>>

    /**
     * 添加一个新的订阅Key，并将其设为当前激活。
     * @param key 要添加的订阅Key。
     */
    suspend fun addSubscriptionKey(key: String)

    /**
     * 设置并持久化当前激活的订阅Key。
     * @param key 要设为激活的订阅Key。
     */
    suspend fun setActiveSubscriptionKey(key: String)

    /**
     * 生成一个随机的订阅ID。
     * @return 随机生成的UUID字符串。
     */
    fun generateRandomSubscriptionId(): String
}
