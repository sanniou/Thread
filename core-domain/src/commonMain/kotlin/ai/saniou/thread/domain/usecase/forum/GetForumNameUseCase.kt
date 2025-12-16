package ai.saniou.thread.domain.usecase.forum

import ai.saniou.thread.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow

class GetForumNameUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(sourceId: String, fid: String): Flow<String?> {
        return forumRepository.getForumName(sourceId, fid)
    }
}
