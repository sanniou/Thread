package ai.saniou.thread.domain.model

/**
 * 一个通用的分页结果容器，用于在 Source 和 Repository 之间传递数据和游标。
 *
 * @param T 数据类型
 * @param data 当前页的数据列表
 * @param prevCursor 上一页的游标 (null 表示没有上一页)
 * @param nextCursor 下一页的游标 (null 表示没有下一页)
 */
data class PagedResult<T>(
    val data: List<T>,
    val prevCursor: String? = null,
    val nextCursor: String? = null
)