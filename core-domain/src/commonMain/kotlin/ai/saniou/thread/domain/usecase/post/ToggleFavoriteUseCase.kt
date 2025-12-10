package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.repository.FavoriteRepository

/**
 * 切换一个板块的收藏状态
 */
class ToggleFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(sourceId: String, forum: Forum) {
        favoriteRepository.toggleFavorite(sourceId, forum)
    }
}
