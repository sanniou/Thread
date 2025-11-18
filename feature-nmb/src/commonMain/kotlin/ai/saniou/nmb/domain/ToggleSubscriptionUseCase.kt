package ai.saniou.nmb.domain

import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.storage.SubscriptionStorage

/**
 * 封装切换帖子订阅状态的业务逻辑
 *
 * @property nmbXdApi 用于调用收藏接口
 * @property subscriptionStorage 用于获取订阅ID
 */
class ToggleSubscriptionUseCase(
    private val nmbXdApi: NmbXdApi,
    private val subscriptionStorage: SubscriptionStorage
) {
    /**
     * 执行切换订阅状态的操作
     *
     * @param threadId 目标帖子的ID
     * @param isCurrentlySubscribed 当前是否已订阅
     * @return 返回一个 [Result] 对象，成功时包含操作结果消息，失败时包含异常
     */
    suspend operator fun invoke(threadId: Long, isCurrentlySubscribed: Boolean): Result<String> {
        return try {
            val subscriptionId = subscriptionStorage.subscriptionId.value
                ?: throw IllegalStateException("订阅ID未加载")

            val resultMessage = if (!isCurrentlySubscribed) {
                // 如果当前未订阅，则执行添加订阅操作
                nmbXdApi.addFeed(subscriptionId, threadId)
            } else {
                // 如果当前已订阅，则执行取消订阅操作
                nmbXdApi.delFeed(subscriptionId, threadId)
            }
            Result.success(resultMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
