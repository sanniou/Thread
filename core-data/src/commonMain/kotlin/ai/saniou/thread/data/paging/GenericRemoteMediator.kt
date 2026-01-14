package ai.saniou.thread.data.paging

import ai.saniou.thread.db.Database
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator

/**
 * 通用 RemoteMediator 实现 (Keyset Paging 适配版)
 *
 * 封装了 RemoteMediator 的通用逻辑，包括：
 * 1. 键值解析 (Key Resolution): 根据 LoadType 确定加载的 Key。
 * 2. 策略检查 (Policy Check): 根据 DataPolicy 决定是否跳过网络请求。
 * 3. 网络请求 (Network Fetch): 执行数据获取。
 * 4. 事务处理 (Transaction): 在事务中保存数据并更新 RemoteKeys。
 *
 * @param Key 分页键类型 (例如 Int, Long, String)
 * @param FetcherValue 列表项类型 (例如 ThreadReply)，用于 PagingState 和 RemoteKeyStrategy
 * @param ResponseType 网络响应的数据类型 (例如 Thread DTO 或 List<Forum>)
 * @param db 数据库实例，用于事务控制
 * @param dataPolicy 数据加载策略
 * @param initialKey 初始键值 (例如第一页页码或最新时间戳)
 * @param remoteKeyStrategy 远程键策略，负责键的获取和存储
 * @param fetcher 数据获取函数，返回 Result<ResponseType>
 * @param saver 数据保存函数，在事务中执行，接收 ResponseType
 * @param itemsExtractor 从 ResponseType 中提取 List<Value>，用于计算 nextKey
 * @param endOfPaginationReached 判断分页是否结束的函数
 * @param cacheChecker (可选) 缓存检查函数，用于 CACHE_ELSE_NETWORK 策略。如果返回 true，则跳过网络请求。
 * @param keyExtractor 从 Value 中提取 Key 的函数，用于计算 nextKey/prevKey
 */
class GenericRemoteMediator<Key : Any, PagerValue : Any, FetcherValue : Any>(
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val initialKey: Key,
    private val remoteKeyStrategy: RemoteKeyStrategy<Key, PagerValue>,
    private val fetcher: suspend (key: Key) -> Result<List<FetcherValue>>,
    private val saver: suspend (List<FetcherValue>, Key, LoadType) -> Unit,
    private val itemsExtractor: (List<FetcherValue>) -> List<FetcherValue>,
    private val endOfPaginationReached: (List<FetcherValue>) -> Boolean,
    private val cacheChecker: (suspend (Key) -> Boolean)? = null,
    private val keyExtractor: ((FetcherValue) -> Key)? = null,
    private val nextKeyProvider: ((Key, List<FetcherValue>) -> Key?)? = null,
    private val prevKeyProvider: ((Key, List<FetcherValue>) -> Key?)? = null,
) : RemoteMediator<Key, PagerValue>() {

    init {
        if (dataPolicy == DataPolicy.CACHE_ELSE_NETWORK) {
            requireNotNull(cacheChecker) {
                "cacheChecker must be provided when using DataPolicy.CACHE_ELSE_NETWORK"
            }
        }
        require(keyExtractor != null || nextKeyProvider != null) {
            "Either keyExtractor or nextKeyProvider must be provided"
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Key, PagerValue>,
    ): MediatorResult {
        return try {
            val key: Key = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKey = remoteKeyStrategy.getKeyClosestToCurrentPosition(state)
                    remoteKey ?: initialKey
                }

                LoadType.PREPEND -> {
                    val remoteKey = remoteKeyStrategy.getKeyForFirstItem(state)
                    remoteKey
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                }

                LoadType.APPEND -> {
                    val remoteKey = remoteKeyStrategy.getKeyForLastItem(state)
                    remoteKey
                        ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                }
            }

            if (loadType != LoadType.REFRESH && dataPolicy == DataPolicy.CACHE_ELSE_NETWORK && cacheChecker != null) {
                if (cacheChecker.invoke(key)) {
                    return MediatorResult.Success(endOfPaginationReached = false)
                }
            } else if (dataPolicy == DataPolicy.CACHE_ONLY) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            fetcher(key).fold(
                onSuccess = { responseData ->
                    val endOfPagination = endOfPaginationReached(responseData)
                    val items = itemsExtractor(responseData)

                    db.transaction {
                        saver(responseData, key, loadType)

                        val prevKey = if (prevKeyProvider != null) {
                            prevKeyProvider.invoke(key, items)
                        } else if (keyExtractor != null && items.isNotEmpty()) {
                            keyExtractor.invoke(items.first())
                        } else {
                            null
                        }

                        val nextKey = if (nextKeyProvider != null) {
                            nextKeyProvider.invoke(key, items)
                        } else if (keyExtractor != null && items.isNotEmpty()) {
                            keyExtractor.invoke(items.last())
                        } else {
                            null
                        }

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
