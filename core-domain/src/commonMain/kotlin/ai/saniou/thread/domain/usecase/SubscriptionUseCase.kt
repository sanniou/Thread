package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.SubscriptionRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * 获取订阅内容流的业务用例。
 */
class GetSubscriptionFeedUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(subscriptionKey: String): Flow<PagingData<Post>> {
        return subscriptionRepository.getSubscriptionFeed(subscriptionKey)
    }
}

/**
 * 切换订阅状态的业务用例。
 */
class ToggleSubscriptionUseCase(private val subscriptionRepository: SubscriptionRepository) {
    suspend operator fun invoke(subscriptionKey: String, subscriptionId: String, isSubscribed: Boolean): Result<String> {
        return subscriptionRepository.toggleSubscription(subscriptionKey, subscriptionId, isSubscribed)
    }
}

/**
 * 检查帖子是否已订阅的业务用例。
 */
class IsSubscribedUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(subscriptionKey: String, postId: String): Flow<Boolean> {
        return subscriptionRepository.isSubscribed(subscriptionKey, postId)
    }
}

/**
 * 同步本地订阅记录到远程的业务用例。
 * 这是一个明确的业务动作：用户可能在离线时进行了订阅，上线后需要同步。
 */
class SyncLocalSubscriptionsUseCase(private val subscriptionRepository: SubscriptionRepository) {
    suspend operator fun invoke(subscriptionKey: String) {
        if (subscriptionRepository.hasLocalSubscriptions(subscriptionKey)) {
            subscriptionRepository.syncLocalSubscriptions(subscriptionKey)
        }
    }
}