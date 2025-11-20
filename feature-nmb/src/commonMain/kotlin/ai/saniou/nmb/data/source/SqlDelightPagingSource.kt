package ai.saniou.nmb.data.source

import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterBase
import app.cash.sqldelight.TransactionCallbacks
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

/**
 * 一个通用的 PagingSource 实现，用于将 SQLDelight 的分页查询与 AndroidX Paging 3 集成。
 *
 * 这个 PagingSource 支持两种分页模式：
 * 1.  **基于页码 (Page-based)**: 直接查询数据库中带 `page` 字段的表。
 * 2.  **基于偏移量 (Limit/Offset-based)**: 用于没有 `page` 字段的查询，通过页码和页面大小计算偏移量。
 *
 * 它还支持：
 * - 在指定的 CoroutineContext (如 Dispatchers.IO) 中执行数据库查询。
 * - 通过监听一个 count 查询，在底层数据发生变化时自动失效，从而触发数据刷新。
 *
 * @param Value 要分页的数据类型。
 * @param transacter SQLDelight 数据库的 Transacter。
 * @param context 执行查询的 CoroutineContext。
 * @param countQueryProvider 返回总数查询的函数，用于数据变化监听。
 * @param limitOffsetQueryProvider (可选) 提供基于 limit/offset 的查询。
 * @param pageQueryProvider (可选) 提供基于 page 的查询。
 *
 * @throws IllegalArgumentException 如果没有提供任何 queryProvider，或者同时提供了两者。
 */
class SqlDelightPagingSource<Value : Any>(
    private val transacter: Transacter,
    private val context: CoroutineContext,
    private val countQueryProvider: () -> Query<Long>,
    private val limitOffsetQueryProvider: ((limit: Long, offset: Long) -> Query<Value>)? = null,
    private val pageQueryProvider: ((page: Int) -> Query<Value>)? = null,
) : PagingSource<Int, Value>() {

    init {
        require(limitOffsetQueryProvider != null || pageQueryProvider != null) {
            "Either limitOffsetQueryProvider or pageQueryProvider must be provided."
        }
        require(limitOffsetQueryProvider == null || pageQueryProvider == null) {
            "Cannot provide both limitOffsetQueryProvider and pageQueryProvider."
        }
    }

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
            withContext(context) {
                val pageNumber = params.key ?: 1

                val data = if (limitOffsetQueryProvider != null) {
                    // Limit/Offset 模式
                    val loadSize = params.loadSize
                    val offset = ((pageNumber - 1) * loadSize).toLong()
                    transacter.transactionWithResult {
                        limitOffsetQueryProvider.invoke(loadSize.toLong(), offset).executeAsList()
                    }
                } else {
                    // Page-based 模式
                    // 注意：此模式忽略 params.loadSize，因为数据库中的“页”大小是固定的。
                    transacter.transactionWithResult {
                        pageQueryProvider!!.invoke(pageNumber).executeAsList()
                    }
                }

                val prevKey = if (pageNumber == 1) null else pageNumber - 1
                val nextKey = if (data.isEmpty()) {
                    // 如果返回数据为空，则认为是最后一页
                    null
                } else {
                    pageNumber + 1
                }

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

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        // 当数据刷新或失效时，Paging 库会调用此方法来决定从哪里开始加载。
        // 我们的策略是找到最近访问的位置 (anchorPosition)，然后从该位置所在的页开始重新加载。
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }
}


internal abstract class QueryPagingSource2<Key : Any, RowType : Any> :
    PagingSource<Key, RowType>(),
    Query.Listener {

    protected var currentQuery: Query<RowType>? by Delegates.observable(null) { _, old, new ->
        old?.removeListener(this)
        new?.addListener(this)
    }

    init {
        registerInvalidatedCallback {
            currentQuery?.removeListener(this)
            currentQuery = null
        }
    }

    final override fun queryResultsChanged() = invalidate()
}


internal class OffsetQueryPagingSource2<RowType : Any>(
    private val queryProvider: (limit: Int, offset: Int) -> Query<RowType>,
    private val countQuery: Query<Int>,
    private val transacter: TransacterBase,
    private val context: CoroutineContext,
    private val initialOffset: Int,
) : QueryPagingSource2<Int, RowType>() {

    override val jumpingSupported get() = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RowType> = withContext(context) {
        val key = params.key ?: initialOffset
        val limit = when (params) {
            is LoadParams.Prepend<*> -> minOf(key, params.loadSize)
            else -> params.loadSize
        }
        val getPagingSourceLoadResult: TransactionCallbacks.() -> LoadResult<Int, RowType> = {
            val count = countQuery.executeAsOne()
            val offset = when (params) {
                is LoadParams.Prepend<*> -> maxOf(0, key - params.loadSize)
                is LoadParams.Append<*> -> key
                is LoadParams.Refresh<*> -> if (key >= count - params.loadSize) maxOf(
                    0,
                    count - params.loadSize
                ) else key

                else -> error("Unknown PagingSourceLoadParams ${params::class}")
            }
            val data = queryProvider(limit, offset)
                .also { currentQuery = it }
                .executeAsList()
            val nextPosToLoad = offset + data.size
            LoadResult.Page(
                data = data,
                prevKey = offset.takeIf { it > 0 && data.isNotEmpty() },
                nextKey = nextPosToLoad.takeIf { data.isNotEmpty() && data.size >= limit && it < count },
                itemsBefore = offset,
                itemsAfter = maxOf(0, count - nextPosToLoad),
            )
        }
        val loadResult = when (transacter) {
            is Transacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
            is SuspendingTransacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
        }
        (if (invalid) LoadResult.Invalid() else loadResult)
    }

    override fun getRefreshKey(state: PagingState<Int, RowType>) =
        state.anchorPosition?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }
}

/**
 * A [PagingSource] that uses page number based pagination.
 *
 * This is a page-keyed PagingSource, which uses page numbers as `LoadParams.key`.
 * It still requires a `limit` (page size), which is provided by the Paging library's `LoadParams.loadSize`,
 * to determine how many items to fetch for a given page.
 *
 * @param queryProvider A function that returns a [Query]. It takes two arguments:
 * `limit` (the number of items to load, i.e., page size) and `page` (the page number to load).
 * @param countQuery A [Query] that returns the total number of items.
 * @param transacter The [TransacterBase] to run the queries in a transaction.
 * @param context The [CoroutineContext] to run the queries on.
 * @param initialPage The initial page number to load (defaults to 1).
 */
internal class PageNumQueryPagingSource2<RowType : Any>(
    private val queryProvider: (limit: Int, page: Int) -> Query<RowType>,
    private val countQuery: Query<Long>,
    private val transacter: TransacterBase,
    private val context: CoroutineContext,
    private val initialPage: Int = 1,
) : QueryPagingSource2<Int, RowType>() {

    override val jumpingSupported get() = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RowType> = withContext(context) {
        val page = params.key ?: initialPage
        val limit = params.loadSize

        val getPagingSourceLoadResult: TransactionCallbacks.() -> LoadResult<Int, RowType> = {
            val count = countQuery.executeAsOne().toInt()
            val data = queryProvider(limit, page)
                .also { currentQuery = it }
                .executeAsList()

            val itemsBefore = (page - 1) * limit
            val itemsAfter = maxOf(0, count - (itemsBefore + data.size))

            LoadResult.Page(
                data = data,
                prevKey = (page - 1).takeIf { it >= initialPage },
                nextKey = (page + 1).takeIf { itemsAfter > 0 },
                itemsBefore = itemsBefore,
                itemsAfter = itemsAfter,
            )
        }
        val loadResult = when (transacter) {
            is Transacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
            is SuspendingTransacter -> transacter.transactionWithResult(bodyWithReturn = getPagingSourceLoadResult)
        }
        (if (invalid) LoadResult.Invalid() else loadResult)
    }

    override fun getRefreshKey(state: PagingState<Int, RowType>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }
}
