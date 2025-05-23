package ai.saniou.nmb.data.storage

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
