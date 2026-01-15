package ai.saniou.thread.data.paging

import ai.saniou.thread.db.Database
import androidx.paging.PagingState
import kotlin.time.Clock

/**
 * 远程键策略接口
 *
 * 定义了如何获取和存储分页键（Remote Keys）。
 * 适配新的 String-based RemoteKey Schema。
 *
 * @param Value 列表项的类型
 */
interface RemoteKeyStrategy<Value : Any> {
    /**
     * 获取列表中第一项对应的 RemoteKey
     */
    suspend fun getKeyForFirstItem(state: PagingState<Int, Value>): String?

    /**
     * 获取列表中最后一项对应的 RemoteKey
     */
    suspend fun getKeyForLastItem(state: PagingState<Int, Value>): String?

    /**
     * 获取最接近当前滚动位置的 RemoteKey
     * 注意：由于新 Schema 移除了 currKey，此方法通常返回 null 或通过 prevKey/nextKey 推断
     * 在 REFRESH 策略为“清空并回到顶部”时，此方法可能不再关键。
     */
    suspend fun getKeyClosestToCurrentPosition(state: PagingState<Int, Value>): String?

    /**
     * 插入或更新 RemoteKey
     */
    suspend fun insertKeys(targetId: String, prevKey: String?, nextKey: String?)

    /**
     * 清除指定 Type 下的所有 Keys
     */
    suspend fun clearKeys()
}

/**
 * 基于 SQLDelight RemoteKeys 表的默认实现
 *
 * @param db 数据库实例
 * @param type 业务类型/源标识 (e.g., "nmb_channel_1")
 * @param itemTargetIdExtractor 从 Value 中提取 targetId 的函数
 */
class DefaultRemoteKeyStrategy<Value : Any>(
    private val db: Database,
    private val type: String,
    private val itemTargetIdExtractor: (Value) -> String
) : RemoteKeyStrategy<Value> {

    private val remoteKeyQueries = db.remoteKeyQueries

    override suspend fun getKeyForFirstItem(state: PagingState<Int, Value>): String? {
        // 获取当前加载页面的第一个 Item
        val item = state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?: return null

        val targetId = itemTargetIdExtractor(item)
        val remoteKey = remoteKeyQueries.remoteKeyByTargetId(targetId, type).executeAsOneOrNull()
        return remoteKey?.prevKey
    }

    override suspend fun getKeyForLastItem(state: PagingState<Int, Value>): String? {
        // 获取当前加载页面的最后一个 Item
        val item = state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?: return null

        val targetId = itemTargetIdExtractor(item)
        val remoteKey = remoteKeyQueries.remoteKeyByTargetId(targetId, type).executeAsOneOrNull()
        return remoteKey?.nextKey
    }

    override suspend fun getKeyClosestToCurrentPosition(state: PagingState<Int, Value>): String? {
        val anchorPosition = state.anchorPosition ?: return null
        val item = state.closestItemToPosition(anchorPosition) ?: return null

        val targetId = itemTargetIdExtractor(item)
        val remoteKey = remoteKeyQueries.remoteKeyByTargetId(targetId, type).executeAsOneOrNull()
        // 优先返回 nextKey 的前一页，或者 prevKey 的后一页？
        // 在 REFRESH = 清空 的策略下，这个返回值主要用于 Paging 内部计算，
        // 但实际上我们 REFRESH 总是传 null，所以这里返回 null 也是安全的。
        // 为了兼容性，尝试返回 nextKey
        return remoteKey?.nextKey
    }

    override suspend fun insertKeys(
        targetId: String,
        prevKey: String?,
        nextKey: String?
    ) {
        remoteKeyQueries.insertOrReplace(
            targetId = targetId,
            type = type,
            prevKey = prevKey,
            nextKey = nextKey,
            lastUpdated = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun clearKeys() {
        remoteKeyQueries.deleteByType(type)
    }
}
