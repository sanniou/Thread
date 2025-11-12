package ai.saniou.nmb.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * 一个通用的 PagingSource 实现，用于将 SQLDelight 的分页查询与 AndroidX Paging 3 集成。
 *
 * @param Value 要分页的数据类型 (例如：数据库实体类 Thread)。
 * @param transacter SQLDelight 数据库的 Transacter，用于在事务中执行查询。
 * @param context 执行数据库查询的 CoroutineContext，通常是 Dispatchers.IO。
 * @param queryProvider 一个函数，接收 limit 和 offset，返回获取一页数据的 SQLDelight 查询。
 */
class SqlDelightPagingSource<Key : Any, Value : Any>(
    private val transacter: Transacter,
    private val context: CoroutineContext,
    private val queryProvider: (limit: Long, offset: Long) -> Query<Value>,
) : PagingSource<Key, Value>() {

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        return try {
            // 在指定的 CoroutineContext (如 Dispatchers.IO) 中执行数据库操作
            withContext(context) {
                // Paging 3 的 Key 通常是页码 (Int) 或位置 (Long)。
                // 我们这里统一按 Int 页码处理，offset/limit 模式更灵活。
                val key = params.key as? Long ?: 0L // key是offset，初始为0
                val loadSize = params.loadSize.toLong()

                // 在事务中执行查询，确保数据一致性
                val data = transacter.transactionWithResult {
                    queryProvider(loadSize, key).executeAsList()
                }

                // 构造 LoadResult.Page
                LoadResult.Page(
                    data = data,
                    // 如果 key 是 0，表示第一页，没有上一页
                    prevKey = if (key == 0L) null else (key - loadSize).coerceAtLeast(0L) as Key?,
                    // 如果返回的数据量小于请求量，说明是最后一页
                    nextKey = if (data.size < loadSize) null else (key + data.size) as Key?
                )
            }
        } catch (e: Exception) {
            // 捕获异常并返回错误状态
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Key, Value>): Key? {
        // 当数据刷新或失效时，Paging 库会调用此方法来决定从哪里开始加载。
        // 一个常见的策略是找到最近访问的位置 (anchorPosition)，然后从那里开始加载。
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
                ?: state.closestPageToPosition(anchorPosition)?.nextKey
        } as Key?
    }
}
