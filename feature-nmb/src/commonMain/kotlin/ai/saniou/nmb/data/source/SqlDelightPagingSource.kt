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
 * 这个 PagingSource 支持：
 * - 基于 offset/limit 的分页。
 * - 在指定的 CoroutineContext (如 Dispatchers.IO) 中执行数据库查询。
 * - 通过监听一个 count 查询，在底层数据发生变化时自动失效，从而触发数据刷新。
 *
 * @param Value 要分页的数据类型 (例如：数据库实体类 Thread)。
 * @param transacter SQLDelight 数据库的 Transacter，用于在事务中执行查询。
 * @param context 执行数据库查询的 CoroutineContext，通常是 Dispatchers.IO。
 * @param countQueryProvider 一个函数，返回一个用于获取总数或监听数据变化的查询。
 *                           当此查询的结果集变化时，PagingSource 会自动失效。
 *                           通常这是一个 `COUNT(*)` 查询。
 * @param queryProvider 一个函数，接收 limit 和 offset，返回获取一页数据的 SQLDelight 查询。
 */
class SqlDelightPagingSource<Value : Any>(
    private val transacter: Transacter,
    private val context: CoroutineContext,
    private val countQueryProvider: () -> Query<Long>,
    private val queryProvider: (limit: Long, offset: Long) -> Query<Value>,
) : PagingSource<Int, Value>() {

    private val listener = Query.Listener {
        invalidate()
    }

    init {
        // 当 PagingSource 创建时，开始监听数据库变化
        val query = countQueryProvider()
        query.addListener(listener)
        // 当 PagingSource 失效时，移除监听器以防止内存泄漏
        registerInvalidatedCallback {
            query.removeListener(listener)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        return try {
            // 在指定的 CoroutineContext (如 Dispatchers.IO) 中执行数据库操作
            withContext(context) {
                val offset = params.key ?: 0
                val loadSize = params.loadSize

                // 在事务中执行查询，确保数据一致性
                val data = transacter.transactionWithResult {
                    queryProvider(loadSize.toLong(), offset.toLong()).executeAsList()
                }

                LoadResult.Page(
                    data = data,
                    // 如果 offset 是 0，表示第一页，没有上一页
                    prevKey = if (offset == 0) null else (offset - loadSize).coerceAtLeast(0),
                    // 如果返回的数据量小于请求量，说明是最后一页
                    nextKey = if (data.size < loadSize) null else offset + data.size
                )
            }
        } catch (e: Exception) {
            // 捕获异常并返回错误状态
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        // 当数据刷新或失效时，Paging 库会调用此方法来决定从哪里开始加载。
        // 我们的策略是找到最近访问的位置 (anchorPosition)，然后从该位置所在的页开始重新加载。
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { page ->
                page.prevKey?.plus(state.config.pageSize) ?: page.nextKey?.minus(state.config.pageSize)
            }
        }
    }
}
