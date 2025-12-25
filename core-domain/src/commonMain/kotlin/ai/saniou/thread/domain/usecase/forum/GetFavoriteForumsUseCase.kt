package ai.saniou.thread.domain.usecase.forum

import ai.saniou.thread.domain.model.forum.Channel as Forum
import ai.saniou.thread.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

/**
 * 获取指定来源的所有收藏板块
 */
class GetFavoriteForumsUseCase(private val favoriteRepository: FavoriteRepository) {
    operator fun invoke(sourceId: String): Flow<List<Forum>> {
        return favoriteRepository.getFavoriteChannels(sourceId)
    }
}
