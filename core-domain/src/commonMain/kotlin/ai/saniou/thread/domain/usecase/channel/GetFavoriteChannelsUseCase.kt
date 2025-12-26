package ai.saniou.thread.domain.usecase.channel

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

/**
 * 获取指定来源的所有收藏板块
 */
class GetFavoriteChannelsUseCase(private val favoriteRepository: FavoriteRepository) {
    operator fun invoke(sourceId: String): Flow<List<Channel>> {
        return favoriteRepository.getFavoriteChannels(sourceId)
    }
}
