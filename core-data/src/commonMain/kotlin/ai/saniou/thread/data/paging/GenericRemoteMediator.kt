package ai.saniou.thread.data.paging

import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.PagedResult
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
 * @param PagerValue 列表项类型 (例如 ThreadReply)，用于 PagingState 和 RemoteKeyStrategy
 * @param FetcherValue 列表项类型 (例如 Thread DTO)，通常与 PagerValue 相同，或者是其 DTO 形式
 * @param db 数据库实例，用于事务控制
 * @param dataPolicy 数据加载策略
 * @param remoteKeyStrategy 远程键策略，负责键的获取和存储
 * @param fetcher 数据获取函数，返回 Result<PagedResult<FetcherValue>>
 * @param saver 数据保存函数，在事务中执行，接收 List<FetcherValue>
 * @param cacheChecker (可选) 缓存检查函数，用于 CACHE_ELSE_NETWORK 策略。如果返回 true，则跳过网络请求。
 * @param itemTargetIdExtractor 从 FetcherValue 中提取 targetId 的函数，用于存储 RemoteKey
 */
class GenericRemoteMediator<PagerValue : Any, FetcherValue : Any>(
    private val db: Database,
    private val dataPolicy: DataPolicy,
    private val remoteKeyStrategy: RemoteKeyStrategy<PagerValue>,
    private val fetcher: suspend (cursor: String?) -> Result<PagedResult<FetcherValue>>,
    private val saver: suspend (List<FetcherValue>, LoadType) -> Unit,
    private val itemTargetIdExtractor: (FetcherValue) -> String,
    private val cacheChecker: (suspend (String?) -> Boolean)? = null,
) : RemoteMediator<Int, PagerValue>() {

    init {
        if (dataPolicy == DataPolicy.CACHE_ELSE_NETWORK) {
            requireNotNull(cacheChecker) {
                "cacheChecker must be provided when using DataPolicy.CACHE_ELSE_NETWORK"
            }
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PagerValue>,
    ): MediatorResult {
        return try {
            val cursor: String? = when (loadType) {
                LoadType.REFRESH -> {
                    // REFRESH 总是从头开始加载 (null)
                    // 如果需要支持 anchorPosition 刷新，逻辑会非常复杂且容易出错。
                    // 这里的策略是：下拉刷新 = 回到顶部并获取最新数据。
                    null
                }

                LoadType.PREPEND -> {
                    val remoteKey = remoteKeyStrategy.getKeyForFirstItem(state)
                    // 如果 remoteKey 为 null，说明没有上一页了 (endOfPaginationReached)
                    // 或者数据为空。
                    // 对于 PREPEND，如果 key 为 null，通常意味着到了顶部。
                    remoteKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val remoteKey = remoteKeyStrategy.getKeyForLastItem(state)
                    // 如果 remoteKey 为 null，说明没有下一页了
                    remoteKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            if (loadType != LoadType.REFRESH && dataPolicy == DataPolicy.CACHE_ELSE_NETWORK && cacheChecker != null) {
                if (cacheChecker.invoke(cursor)) {
                    return MediatorResult.Success(endOfPaginationReached = false)
                }
            } else if (dataPolicy == DataPolicy.CACHE_ONLY) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            fetcher(cursor).fold(
                onSuccess = { pagedResult ->
                    val items = pagedResult.data
                    val endOfPagination = pagedResult.nextCursor == null

                    db.transaction {
                        // 如果是 REFRESH，清除旧数据和 Keys
                        if (loadType == LoadType.REFRESH) {
                            remoteKeyStrategy.clearKeys()
                            // 注意：saver 内部应该负责清除旧数据 (例如 clearPage = true)
                            // 或者我们应该在这里提供一个 clearData 的回调？
                            // 目前 ChannelRepositoryImpl 的 saver 在 REFRESH 时会 clearPage。
                            // 但为了更通用，也许应该在这里显式调用？
                            // 鉴于 saver 已经接收了 loadType，让 saver 决定是否清除是合理的。
                        }

                        saver(items, loadType)

                        // 为每个 Item 插入 RemoteKey
                        items.forEach { item ->
                            remoteKeyStrategy.insertKeys(
                                targetId = itemTargetIdExtractor(item),
                                prevKey = pagedResult.prevCursor,
                                nextKey = pagedResult.nextCursor
                            )
                        }
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
