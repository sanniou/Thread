package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

/**
 * 检查板块是否已收藏用例
 */
class IsFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(sourceId: String, channelId: String): Flow<Boolean> {
        return favoriteRepository.isFavorite(sourceId, channelId)
    }
}
