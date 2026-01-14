package ai.saniou.thread.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * 一个通用的 Keyset PagingSource 实现，用于将 SQLDelight 的 Keyset 分页查询与 AndroidX Paging 3 集成。
 *
 * @param Key 分页键类型 (例如 Int, Long, String)。
 * @param Value 要分页的数据类型。
 * @param transacter SQLDelight 数据库的 Transacter。
 * @param context 执行查询的 CoroutineContext。
 * @param queryProvider 提供基于 Key 的查询。接收当前 Key (如果为 null 则表示第一页) 和 limit。
 * @param countQueryProvider 返回总数查询的函数，用于数据变化监听。
 * @param keyExtractor 从 Value 中提取 Key 的函数，用于计算 nextKey。
 */
class KeysetPagingSource<Key : Any, Value : Any>(
    private val transacter: SuspendingTransacter,
    private val context: CoroutineContext,
    private val queryProvider: (key: Key?, limit: Long) -> Query<Value>,
    private val countQueryProvider: () -> Query<Long>,
    private val keyExtractor: (Value) -> Key
) : PagingSource<Key, Value>() {

    private val listener = Query.Listener {
        invalidate()
    }

    init {
        val query = countQueryProvider()
        query.addListener(listener)
        registerInvalidatedCallback {
            query.removeListener(listener)
        }
    }

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        return try {
            withContext(context) {
                val key = params.key
                val loadSize = params.loadSize.toLong()

                val data = transacter.transactionWithResult {
                    queryProvider(key, loadSize).executeAsList()
                }

                val nextKey = if (data.size < loadSize || data.isEmpty()) {
                    null
                } else {
                    keyExtractor(data.last())
                }

                // Keyset Paging 通常不支持向前翻页 (PREPEND)，除非实现反向查询
                // 这里简化处理，prevKey 设为 null
                val prevKey = null

                LoadResult.Page(
                    data = data,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Key, Value>): Key? {
        // 对于 Keyset Paging，刷新时通常希望保持当前位置
        // 但最简单的策略是返回 null (回到顶部) 或者 anchorPosition 对应的 Key
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)?.let { item ->
                keyExtractor(item)
            }
        }
    }
}