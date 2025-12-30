package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Channel
import kotlinx.coroutines.flow.Flow

/**
 * 收藏仓库接口，定义了领域层需要对收藏的板块进行的操作
 */
interface FavoriteRepository {
    /**
     * 获取指定来源的所有收藏板块
     * @param sourceId 信息源ID
     */
    fun getFavoriteChannels(sourceId: String): Flow<List<Channel>>

    /**
     * 切换一个板块的收藏状态
     * @param sourceId 信息源ID
     * @param channel 要切换的板块
     */
    suspend fun toggleFavorite(sourceId: String, channel: Channel)

    /**
     * 检查一个板块是否被收藏
     * @param sourceId 信息源ID
     * @param channel 板块ID
     */
    fun isFavorite(sourceId: String, channel: String): Flow<Boolean>
}
