package ai.saniou.thread.domain.usecase.thread

import ai.saniou.thread.domain.model.history.HistoryPost
import ai.saniou.thread.domain.repository.HistoryRepository
import ai.saniou.thread.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Instant

class UpdateThreadLastAccessTimeUseCase(
    private val threadRepository: ThreadRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(sourceId: String, threadId: String, time: Long) {
        val post = threadRepository.getThreadDetail(sourceId, threadId).firstOrNull()
        if (post != null) {
            historyRepository.addToHistory(HistoryPost(post, Instant.fromEpochMilliseconds(time)))
        }
    }
}
