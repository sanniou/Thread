package ai.saniou.nmb.data.storage

import ai.saniou.nmb.data.entity.ForumCategory
import io.github.irgaly.kottage.getOrNull
import io.github.irgaly.kottage.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

/**
 * 管理论坛分类数据的持久化存储
 */
class CategoryStorage(scope: CoroutineScope) : BasicStorage(scope, "forum-categories") {

    private val _lastOpenedForumId = MutableStateFlow<Long?>(null)
    val lastOpenedForumId = _lastOpenedForumId.asStateFlow()

    private val _lastOpenedCategoryId = MutableStateFlow<Long?>(null)
    val lastOpenedCategoryId = _lastOpenedCategoryId.asStateFlow()

    /**
     * 检查缓存的分类数据是否过期（超过1天）
     */
    suspend fun isCategoryDataExpired(): Boolean {
        val lastUpdateTime = storage.getOrNull<CategoryCacheInfo>("cache_info")?.lastUpdateTime
        if (lastUpdateTime == null) return true

        val now = Clock.System.now()
        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastUpdateTime)
        return now - lastUpdateInstant > 1.days
    }

    /**
     * 获取缓存的分类数据
     */
    suspend fun getCachedCategories(): List<ForumCategory>? {
        return storage.getOrNull<List<ForumCategory>>("categories")
    }

    /**
     * 保存分类数据到缓存
     */
    suspend fun saveCategories(categories: List<ForumCategory>) {
        storage.put("categories", categories)
        storage.put(
            "cache_info", CategoryCacheInfo(
                lastUpdateTime = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    /**
     * 保存最后打开的论坛ID
     */
    suspend fun saveLastOpenedForumId(forumId: Long?) {
        forumId?.run {
            storage.put("last_opened_forum_id", forumId)
        } ?: run {
            storage.remove("last_opened_forum_id")
        }
        _lastOpenedForumId.value = forumId
    }

    /**
     * 获取最后打开的论坛ID
     */
    suspend fun getLastOpenedForumId(): Long? {
        val id = storage.getOrNull<Long>("last_opened_forum_id")
        val result = id
        _lastOpenedForumId.value = result
        return result
    }

    /**
     * 保存最后打开的分类ID
     */
    suspend fun saveLastOpenedCategoryId(categoryId: Long?) {
        categoryId?.run {
            storage.put("last_opened_category_id", categoryId)
        } ?: run {
            storage.remove("last_opened_category_id")
        }
        _lastOpenedCategoryId.value = categoryId
    }

    /**
     * 获取最后打开的分类ID
     */
    suspend fun getLastOpenedCategoryId(): Long? {
        val id = storage.getOrNull<Long>("last_opened_category_id")
        val result = id
        _lastOpenedCategoryId.value = result
        return result
    }

    @Serializable
    private data class CategoryCacheInfo(
        val lastUpdateTime: Long
    )

}
