package ai.saniou.nmb.data.storage

import ai.saniou.nmb.data.entity.ForumCategory
import io.github.irgaly.kottage.Kottage
import io.github.irgaly.kottage.KottageStorage
import io.github.irgaly.kottage.getOrNull
import io.github.irgaly.kottage.platform.KottageContext
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
class CategoryStorage(
    private val scope: CoroutineScope
) {
    private val directoryPath by lazy { getStorageDirectory() }
    private val kottage: Kottage by lazy {
        Kottage(
            name = "nmb-category-storage",
            directoryPath = directoryPath,
            environment = KottageEnvironment(),
            scope = scope
        )
    }

    private val storage: KottageStorage by lazy {
        kottage.storage("forum-categories")
    }

    private val _lastOpenedForumId = MutableStateFlow<String?>(null)
    val lastOpenedForumId = _lastOpenedForumId.asStateFlow()

    private val _lastOpenedCategoryId = MutableStateFlow<String?>(null)
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
    suspend fun saveLastOpenedForumId(forumId: String?) {
        storage.put("last_opened_forum_id", forumId ?: "")
        _lastOpenedForumId.value = forumId
    }

    /**
     * 获取最后打开的论坛ID
     */
    suspend fun getLastOpenedForumId(): String? {
        val id = storage.getOrNull<String>("last_opened_forum_id")
        val result = if (id.isNullOrEmpty()) null else id
        _lastOpenedForumId.value = result
        return result
    }

    /**
     * 保存最后打开的分类ID
     */
    suspend fun saveLastOpenedCategoryId(categoryId: String?) {
        storage.put("last_opened_category_id", categoryId ?: "")
        _lastOpenedCategoryId.value = categoryId
    }

    /**
     * 获取最后打开的分类ID
     */
    suspend fun getLastOpenedCategoryId(): String? {
        val id = storage.getOrNull<String>("last_opened_category_id")
        val result = if (id.isNullOrEmpty()) null else id
        _lastOpenedCategoryId.value = result
        return result
    }

    /**
     * 关闭存储
     */
    suspend fun close() {
        kottage.close()
    }

    @Serializable
    private data class CategoryCacheInfo(
        val lastUpdateTime: Long
    )

    /**
     * 创建 KottageEnvironment
     */
    private fun KottageEnvironment(): io.github.irgaly.kottage.KottageEnvironment {
        return io.github.irgaly.kottage.KottageEnvironment(
            context = KottageContext()
        )
    }
}
