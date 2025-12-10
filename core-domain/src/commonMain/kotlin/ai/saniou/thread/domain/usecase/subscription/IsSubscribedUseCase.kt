package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class IsSubscribedUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(subscriptionKey: String, postId: String): Flow<Boolean> {
        return subscriptionRepository.isSubscribed(subscriptionKey, postId)
    }
}
