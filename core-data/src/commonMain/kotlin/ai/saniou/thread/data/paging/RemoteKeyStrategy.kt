package ai.saniou.thread.data.paging

import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.RemoteKeys
import app.cash.paging.PagingState
import kotlin.time.Clock

/**
 * 远程键策略接口
 *
 * 定义了如何获取和存储分页键（Remote Keys）。
 * 这允许 GenericRemoteMediator 适应不同的键类型（如页码、游标）和存储机制。
 *
 * @param Key 分页键的类型 (例如 Int, String)
 * @param Value 列表项的类型
 */
interface RemoteKeyStrategy<Key : Any, Value : Any> {
    /**
     * 获取列表中第一项对应的 RemoteKey
     */
    suspend fun getKeyForFirstItem(state: PagingState<Key, Value>): RemoteKeys?

    /**
     * 获取列表中最后一项对应的 RemoteKey
     */
    suspend fun getKeyForLastItem(state: PagingState<Key, Value>): RemoteKeys?

    /**
     * 获取最接近当前滚动位置的 RemoteKey
     */
    suspend fun getKeyClosestToCurrentPosition(state: PagingState<Key, Value>): RemoteKeys?

    /**
     * 插入或更新 RemoteKey
     */
    fun insertKeys(key: Key, prevKey: Key?, nextKey: Key?, endOfPagination: Boolean)
}

/**
 * 基于 SQLDelight RemoteKeys 表的默认实现
 *
 * 适用于使用 Long 类型作为分页键（如页码）的场景。
 *
 * @param db 数据库实例
 * @param type RemoteKey 类型 (例如 THREAD, FORUM)
 * @param id 关联的 ID (例如 threadId, forumId)
 * @param itemIdExtractor 从列表项中提取 ID 的函数，用于查询 RemoteKey
 */
class DefaultRemoteKeyStrategy<Value : Any>(
    private val db: Database,
    private val type: RemoteKeyType,
    private val id: String,
    private val itemIdExtractor: (Value) -> String
) : RemoteKeyStrategy<Int, Value> {

    private val remoteKeyQueries = db.remoteKeyQueries

    override suspend fun getKeyForFirstItem(state: PagingState<Int, Value>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { item ->
                remoteKeyQueries.getRemoteKeyById(type, itemIdExtractor(item))
                    .executeAsOneOrNull()
            }
    }

    override suspend fun getKeyForLastItem(state: PagingState<Int, Value>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { item ->
                remoteKeyQueries.getRemoteKeyById(type, itemIdExtractor(item))
                    .executeAsOneOrNull()
            }
    }

    override suspend fun getKeyClosestToCurrentPosition(state: PagingState<Int, Value>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { item ->
                remoteKeyQueries.getRemoteKeyById(type, itemIdExtractor(item))
                    .executeAsOneOrNull()
            }
        }
    }

    override fun insertKeys(
        key: Int,
        prevKey: Int?,
        nextKey: Int?,
        endOfPagination: Boolean
    ) {
        val finalNextKey = if (endOfPagination) null else nextKey
        remoteKeyQueries.insertKey(
            type = type,
            id = id,
            prevKey = prevKey?.toLong(),
            currKey = key.toLong(),
            nextKey = finalNextKey?.toLong(),
            updateAt = Clock.System.now().toEpochMilliseconds(),
        )
    }
}