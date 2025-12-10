package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.model.Post
import ai.saniou.thread.domain.repository.SubscriptionRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetSubscriptionFeedUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(subscriptionKey: String): Flow<PagingData<Post>> {
        return subscriptionRepository.getSubscriptionFeed(subscriptionKey)
    }
}
