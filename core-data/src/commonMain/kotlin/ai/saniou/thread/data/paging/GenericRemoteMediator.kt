package ai.saniou.thread.data.paging

import ai.saniou.thread.db.Database
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator


/**
 * 通用 RemoteMediator 实现
 *
 * 封装了 RemoteMediator 的通用逻辑，包括：
 * 1. 键值解析 (Key Resolution): 根据 LoadType 确定加载的页码/键。
 * 2. 策略检查 (Policy Check): 根据 DataPolicy 决定是否跳过网络请求。
 * 3. 网络请求 (Network Fetch): 执行数据获取。
 * 4. 事务处理 (Transaction): 在事务中保存数据并更新 RemoteKeys。
 *
 * @param Key 分页键类型 (例如 Int)
 * @param Value 列表项类型 (例如 ThreadReply)，用于 PagingState 和 RemoteKeyStrategy
 * @param ResponseType 网络响应的数据类型 (例如 Thread DTO 或 List<Forum>)
 * @param db 数据库实例，用于事务控制
 * @param dataPolicy 数据加载策略
 * @param initialKey 初始键值 (例如第一页页码)
 * @param remoteKeyStrategy 远程键策略，负责键的获取和存储
 * @param fetcher 数据获取函数，返回 SaniouResponse<ResponseType>
 * @param saver 数据保存函数，在事务中执行，接收 ResponseType
 * @param itemsExtractor 从 ResponseType 中提取 List<Value>，用于判断分页是否结束
 * @param cacheChecker (可选) 缓存检查函数，用于 CACHE_ELSE_NETWORK 策略。如果返回 true，则跳过网络请求。
 */
class GenericRemoteMediator<Key : Any, Value : Any, ResponseType : Any>(
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialKey: Key,
    private val remoteKeyStrategy: RemoteKeyStrategy<Key, Value>,
    private val fetcher: suspend (key: Key) -> Result<ResponseType>,
    private val saver: suspend (ResponseType, Key, LoadType) -> Unit,
    private val endOfPaginationReached: (ResponseType) -> Boolean,
    private val cacheChecker: (suspend (Key) -> Boolean)? = null,
    private val keyIncrementer: (Key) -> Key,
    private val keyDecrementer: (Key) -> Key,
    private val keyToLong: (Key) -> Long,
    private val longToKey: (Long) -> Key,
) : RemoteMediator<Key, Value>() {

    init {
        if (dataPolicy == DataPolicy.CACHE_ELSE_NETWORK) {
            requireNotNull(cacheChecker) {
                "cacheChecker must be provided when using DataPolicy.CACHE_ELSE_NETWORK"
            }
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Key, Value>,
    ): MediatorResult {
        return try {
            val key: Key = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKey = remoteKeyStrategy.getKeyClosestToCurrentPosition(state)
                    remoteKey?.nextKey?.let { nextKeyLong ->
                        val nextKey = longToKey(nextKeyLong)
                        keyDecrementer(nextKey)
                    } ?: initialKey
                }

                LoadType.PREPEND -> {
                    val remoteKey = remoteKeyStrategy.getKeyForFirstItem(state)
                    val prevKeyLong = remoteKey?.prevKey
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                    longToKey(prevKeyLong)
                }

                LoadType.APPEND -> {
                    val remoteKey = remoteKeyStrategy.getKeyForLastItem(state)
                    val nextKeyLong = remoteKey?.nextKey
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                    longToKey(nextKeyLong)
                }
            }

            // 策略检查
            if (dataPolicy == DataPolicy.CACHE_ELSE_NETWORK && cacheChecker != null) {
                if (cacheChecker.invoke(key)) {
                    return MediatorResult.Success(endOfPaginationReached = false)
                }
            } else if (dataPolicy == DataPolicy.CACHE_ONLY) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            fetcher(key).fold(
                onSuccess = { responseData ->
                    val endOfPagination = endOfPaginationReached(responseData)
                    db.transaction {
                        saver(responseData, key, loadType)

                        val prevKey = if (key == initialKey) null else keyDecrementer(key)
                        val nextKey = if (endOfPagination) null else keyIncrementer(key)

                        remoteKeyStrategy.insertKeys(key, prevKey, nextKey, endOfPagination)
                    }

                    MediatorResult.Success(endOfPaginationReached = endOfPagination)
                },
                onFailure = { ex ->
                    MediatorResult.Error(ex)
                }
            )
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
