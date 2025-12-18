package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository

class GenerateRandomSubscriptionIdUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(): String {
        return subscriptionRepository.generateRandomSubscriptionId()
    }
}