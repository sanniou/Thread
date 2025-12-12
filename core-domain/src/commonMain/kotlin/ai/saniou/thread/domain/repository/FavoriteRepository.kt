package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Forum
import kotlinx.coroutines.flow.Flow

/**
 * 收藏仓库接口，定义了领域层需要对收藏的板块进行的操作
 */
interface FavoriteRepository {
    /**
     * 获取指定来源的所有收藏板块
     * @param sourceId 信息源ID
     */
    fun getFavoriteForums(sourceId: String): Flow<List<Forum>>

    /**
     * 切换一个板块的收藏状态
     * @param sourceId 信息源ID
     * @param forum 要切换的板块
     */
    suspend fun toggleFavorite(sourceId: String, forum: Forum)

    /**
     * 检查一个板块是否被收藏
     * @param sourceId 信息源ID
     * @param forumId 板块ID
     */
    fun isFavorite(sourceId: String, forumId: String): Flow<Boolean>
}