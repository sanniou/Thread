package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.history.HistoryPost
import ai.saniou.thread.domain.repository.HistoryRepository
import ai.saniou.thread.domain.repository.TopicRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Instant

class UpdateTopicLastAccessTimeUseCase(
    private val topicRepository: TopicRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(sourceId: String, topicId: String, time: Long) {
        val post = topicRepository.getTopicDetail(sourceId, topicId)
            .catch {
                throw it
            }
            .firstOrNull()
        if (post != null) {
            historyRepository.addToHistory(HistoryPost(post, Instant.fromEpochMilliseconds(time)))
        }
    }
}
