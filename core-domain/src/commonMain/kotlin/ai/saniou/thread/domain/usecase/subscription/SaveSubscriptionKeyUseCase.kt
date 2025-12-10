package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository

class SaveSubscriptionKeyUseCase(private val subscriptionRepository: SubscriptionRepository) {
    suspend operator fun invoke(key: String) {
        subscriptionRepository.addSubscriptionKey(key)
    }
}