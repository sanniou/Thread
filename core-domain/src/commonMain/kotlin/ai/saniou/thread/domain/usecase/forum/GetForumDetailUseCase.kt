package ai.saniou.thread.domain.usecase.forum

import ai.saniou.thread.domain.model.forum.Channel as Forum
import ai.saniou.thread.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow

class GetForumDetailUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(sourceId: String, fid: String): Flow<Forum?> {
        return forumRepository.getForumDetail(sourceId, fid)
    }
}
