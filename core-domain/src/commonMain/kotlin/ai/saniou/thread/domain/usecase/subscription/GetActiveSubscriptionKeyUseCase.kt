package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository

class GetActiveSubscriptionKeyUseCase(private val subscriptionRepository: SubscriptionRepository) {
    suspend operator fun invoke(): String? {
        return subscriptionRepository.getActiveSubscriptionKey()
    }
}