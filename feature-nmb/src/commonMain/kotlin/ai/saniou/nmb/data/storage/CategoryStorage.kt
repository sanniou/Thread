package ai.saniou.nmb.data.storage

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.data.entity.ForumDetail
import io.github.irgaly.kottage.KottageList
import io.github.irgaly.kottage.add
import io.github.irgaly.kottage.getOrNull
import io.github.irgaly.kottage.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

/**
 * 管理论坛分类数据的持久化存储
 */
class CategoryStorage(scope: CoroutineScope) : BasicStorage(scope, "forum-categories") {

    private val _lastOpenedForum = MutableStateFlow<ForumDetail?>(null)
    val lastOpenedForum = _lastOpenedForum.asStateFlow()

    private val favoriteForumList: KottageList by lazy {
        storage.list("favorite_forums")
    }

    private val _favoriteForums = MutableStateFlow<List<ForumDetail>>(emptyList())
    private val favoriteForumsLazy = lazy { _favoriteForums.asStateFlow() }
    val favoriteForums: StateFlow<List<ForumDetail>> by favoriteForumsLazy

    suspend fun getFavoriteForums(): StateFlow<List<ForumDetail>> {
        if (!favoriteForumsLazy.isInitialized()) {
            _favoriteForums.value = favoriteForumList.getPageFrom(
                positionId = null,
                pageSize = null
            ).items.map { it.value<ForumDetail>() }
        }
        return favoriteForums
    }

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

    suspend fun changeFavoriteForum(categories: ForumDetail) {
        if (storage.exists("${categories.fGroup}-${categories.id}"))
            removeFavoriteForum(categories)
        else
            addFavoriteForum(categories)
    }

    suspend fun addFavoriteForum(categories: ForumDetail) {
        favoriteForumList.add("${categories.fGroup}-${categories.id}", categories)
        _favoriteForums.value = _favoriteForums.value + categories

    }

    suspend fun removeFavoriteForum(categories: ForumDetail) {
        favoriteForumList.remove("${categories.fGroup}-${categories.id}", true)
        _favoriteForums.value = _favoriteForums.value - categories
    }

    /**
     * 保存最后打开的论坛ID
     */
    suspend fun saveLastOpenedForum(forum: ForumDetail?) {
        forum?.run {
            storage.put("last_opened_forum", forum)
        } ?: run {
            storage.remove("last_opened_forum")
        }
        _lastOpenedForum.value = forum
    }

    /**
     * 获取最后打开的论坛ID
     */
    suspend fun getLastOpenedForum(): ForumDetail? {
        val result = storage.getOrNull<ForumDetail>("last_opened_forum")
        _lastOpenedForum.value = result
        return result
    }

    @Serializable
    private data class CategoryCacheInfo(
        val lastUpdateTime: Long
    )

}
