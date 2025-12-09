package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow

class GetForumDetailUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(fid: Long): Flow<Forum?> {
        return forumRepository.getForumDetail(fid)
    }
}
