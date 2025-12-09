package ai.saniou.nmb.domain

import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.thread.data.source.nmb.NmbSource

/**
 * 封装切换帖子订阅状态的业务逻辑
 *
 * @property nmbXdApi 用于调用收藏接口
 * @property subscriptionStorage 用于获取订阅ID
 * @property nmbRepository 用于更新本地数据库
 */
class ToggleSubscriptionUseCase(
    private val nmbXdApi: NmbXdApi,
    private val subscriptionStorage: SubscriptionStorage,
    private val nmbRepository: NmbSource,
) {
    /**
     * 执行切换订阅状态的操作
     *
     * @param thread 目标帖子
     * @param isCurrentlySubscribed 当前是否已订阅
     * @return 返回一个 [Result] 对象，成功时包含操作结果消息，失败时包含异常
     */
    suspend operator fun invoke(thread: Thread, isCurrentlySubscribed: Boolean): Result<String> {
        return try {
            val subscriptionId = subscriptionStorage.subscriptionId.value
                ?: throw IllegalStateException("订阅ID未加载")

            val resultMessage = if (!isCurrentlySubscribed) {
                val message = nmbXdApi.addFeed(subscriptionId, thread.id)
                nmbRepository.addSubscription(subscriptionId, thread)
                message
            } else {
                val message = nmbXdApi.delFeed(subscriptionId, thread.id)
                nmbRepository.deleteSubscription(subscriptionId, thread.id)
                message
            }
            Result.success(resultMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
