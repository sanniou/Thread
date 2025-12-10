package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveSubscriptionKeyUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(): Flow<String?> {
        return subscriptionRepository.observeActiveSubscriptionKey()
    }
}