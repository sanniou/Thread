package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.repository.FavoriteRepository
import ai.saniou.thread.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * 获取 NMB 板块页所需的所有数据, 包含板块列表和收藏列表
 */
class GetNmbForumPageUseCase(
    private val feedRepository: FeedRepository,
    private val favoriteRepository: FavoriteRepository,
) {
    operator fun invoke(): Flow<Result<ForumPageData>> {
        val forumsFlow = flow { emit(feedRepository.getForums("nmb")) }
        val favoritesFlow = favoriteRepository.getFavoriteForums("nmb")

        return combine(forumsFlow, favoritesFlow) { forumsResult, favorites ->
            forumsResult.map { forums ->
                ForumPageData(
                    forums = forums,
                    favorites = favorites
                )
            }
        }
    }
}

data class ForumPageData(
    val forums: List<Forum>,
    val favorites: List<Forum>,
)
