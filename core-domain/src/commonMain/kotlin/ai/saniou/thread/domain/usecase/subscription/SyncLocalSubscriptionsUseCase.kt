package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository

class SyncLocalSubscriptionsUseCase(private val subscriptionRepository: SubscriptionRepository) {
    suspend operator fun invoke(subscriptionKey: String) {
        if (subscriptionRepository.hasLocalSubscriptions(subscriptionKey)) {
            subscriptionRepository.syncLocalSubscriptions(subscriptionKey)
        }
    }
}
