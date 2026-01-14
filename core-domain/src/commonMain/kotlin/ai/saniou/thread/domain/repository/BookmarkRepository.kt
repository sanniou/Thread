package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.bookmark.Bookmark
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

/**
 * 收藏仓库接口，定义了收藏功能的标准契约。
 */
interface BookmarkRepository {

    /**
     * 获取所有收藏。
     * @return 一个包含所有收藏列表的 Flow。
     */
    fun getBookmarks(query: String? = null, tags: List<String>? = null): PagingSource<Int, Bookmark>

    /**
     * 添加一个收藏。
     * @param bookmark 要添加的收藏对象
     */
    suspend fun addBookmark(bookmark: Bookmark)

    /**
     * 移除一个收藏。
     * @param id 收藏的ID
     */
    suspend fun removeBookmark(id: String)

    /**
     * 检查一个收藏是否存在。
     * @param id 收藏的ID
     * @return 一个布尔值的 Flow，true 表示已收藏。
     */
    fun isBookmarked(id: String): Flow<Boolean>
}
