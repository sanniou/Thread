package ai.saniou.thread.domain.usecase.block

import ai.saniou.thread.domain.model.block.ContentBlock
import ai.saniou.thread.domain.repository.ContentBlockRepository

class AddContentBlockUseCase(
    private val repository: ContentBlockRepository,
) {
    suspend fun keyword(keywords: List<String>): ContentBlock =
        repository.addKeywordRule(keywords)

    suspend fun user(userId: String?, userName: String?): ContentBlock =
        repository.addUserRule(userId, userName)
}
