package ai.saniou.thread.domain.usecase.forum

import ai.saniou.thread.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow

class GetForumNameUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(fid: Long): Flow<String?> {
        return forumRepository.getForumName(fid)
    }
}
