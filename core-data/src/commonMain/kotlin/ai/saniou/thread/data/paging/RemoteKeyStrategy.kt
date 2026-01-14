package ai.saniou.thread.data.paging

import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.db.Database
import androidx.paging.PagingState
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
    suspend fun getKeyForFirstItem(state: PagingState<Key, Value>): Key?

    /**
     * 获取列表中最后一项对应的 RemoteKey
     */
    suspend fun getKeyForLastItem(state: PagingState<Key, Value>): Key?

    /**
     * 获取最接近当前滚动位置的 RemoteKey
     */
    suspend fun getKeyClosestToCurrentPosition(state: PagingState<Key, Value>): Key?

    /**
     * 插入或更新 RemoteKey
     */
    suspend fun insertKeys(key: Key, prevKey: Key?, nextKey: Key?, endOfPagination: Boolean)
}

/**
 * 基于 SQLDelight RemoteKeys 表的默认实现
 *
 * 适用于使用任意类型作为分页键的场景，通过序列化器转换为 String 存储。
 *
 * @param db 数据库实例
 * @param type RemoteKey 类型 (例如 THREAD, FORUM)
 * @param id 关联的 ID (例如 threadId, forumId)
 * @param serializer Key 序列化函数
 * @param deserializer Key 反序列化函数
 */
class DefaultRemoteKeyStrategy<Key : Any, Value : Any>(
    private val db: Database,
    private val type: RemoteKeyType,
    private val id: String,
    private val serializer: (Key) -> String,
    private val deserializer: (String) -> Key
) : RemoteKeyStrategy<Key, Value> {

    private val remoteKeyQueries = db.remoteKeyQueries

    override suspend fun getKeyForFirstItem(state: PagingState<Key, Value>): Key? {
        val remoteKey = remoteKeyQueries.getRemoteKeyById(type, id).executeAsOneOrNull()
        return remoteKey?.prevKey?.let(deserializer)
    }

    override suspend fun getKeyForLastItem(state: PagingState<Key, Value>): Key? {
        val remoteKey = remoteKeyQueries.getRemoteKeyById(type, id).executeAsOneOrNull()
        return remoteKey?.nextKey?.let(deserializer)
    }

    override suspend fun getKeyClosestToCurrentPosition(state: PagingState<Key, Value>): Key? {
        val remoteKey = remoteKeyQueries.getRemoteKeyById(type, id).executeAsOneOrNull()
        return remoteKey?.currKey?.let(deserializer)
    }

    override suspend fun insertKeys(
        key: Key,
        prevKey: Key?,
        nextKey: Key?,
        endOfPagination: Boolean
    ) {
        val finalNextKey = if (endOfPagination) null else nextKey
        remoteKeyQueries.insertKey(
            type = type,
            id = id,
            prevKey = prevKey?.let(serializer),
            currKey = serializer(key),
            nextKey = finalNextKey?.let(serializer),
            updateAt = Clock.System.now().toEpochMilliseconds(),
        )
    }
}