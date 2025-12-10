package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository

class ToggleSubscriptionUseCase(private val subscriptionRepository: SubscriptionRepository) {
    suspend operator fun invoke(subscriptionKey: String, subscriptionId: String, isSubscribed: Boolean): Result<String> {
        return subscriptionRepository.toggleSubscription(subscriptionKey, subscriptionId, isSubscribed)
    }
}
