package ai.saniou.thread.domain.usecase.subscription

import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.SubscriptionRepository
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetSubscriptionFeedUseCase(private val subscriptionRepository: SubscriptionRepository) {
    operator fun invoke(subscriptionKey: String): Flow<PagingData<Topic>> {
        return subscriptionRepository.getSubscriptionFeed(subscriptionKey)
    }
}
