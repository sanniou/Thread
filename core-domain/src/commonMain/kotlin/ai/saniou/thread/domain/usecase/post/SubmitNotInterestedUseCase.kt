package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.repository.ReactionRepository

class SubmitNotInterestedUseCase(
    private val repository: ReactionRepository,
) {
    suspend operator fun invoke(
        sourceId: String,
        topicId: String,
        channelId: String? = null,
        reasonIds: String = "",
        extra: String = "",
        clickTimeMs: Long = 0L,
    ): Result<String> = repository.submitNotInterested(
        sourceId = sourceId,
        topicId = topicId,
        channelId = channelId,
        reasonIds = reasonIds,
        extra = extra,
        clickTimeMs = clickTimeMs,
    )
}
