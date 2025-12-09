package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

/**
 * 收藏仓库接口，定义了收藏功能的标准契约。
 */
interface BookmarkRepository {

    /**
     * 获取所有收藏。
     * @return 一个包含所有收藏列表的 Flow。
     */
    fun getBookmarks(): Flow<List<Bookmark>>

    /**
     * 添加一个收藏。
     * @param postId 帖子ID
     * @param content 帖子内容摘要
     * @param tag 标签 (可选)
     */
    suspend fun addBookmark(postId: String, content: String, tag: String? = null)

    /**
     * 移除一个收藏。
     * @param postId 帖子ID
     */
    suspend fun removeBookmark(postId: String)

    /**
     * 检查一个帖子是否已被收藏。
     * @param postId 帖子ID
     * @return 一个布尔值的 Flow，true 表示已收藏。
     */
    fun isBookmarked(postId: String): Flow<Boolean>
}