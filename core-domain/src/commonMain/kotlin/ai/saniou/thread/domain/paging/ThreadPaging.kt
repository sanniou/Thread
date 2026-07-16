package ai.saniou.thread.domain.paging

import androidx.paging.PagingConfig

const val DEFAULT_THREAD_PAGE_SIZE = 20

/**
 * The shared cache/window policy for SQLDelight + Paging 3 on every target.
 * Two pages are loaded up front, half a page is prefetched, and ten pages are
 * retained to avoid unbounded desktop sessions without making mobile memory
 * behavior platform-specific.
 */
fun threadPagingConfig(
    pageSize: Int = DEFAULT_THREAD_PAGE_SIZE,
    initialLoadPages: Int = 2,
    maxCachedPages: Int = 10,
): PagingConfig {
    require(pageSize > 0) { "pageSize must be positive" }
    require(initialLoadPages > 0) { "initialLoadPages must be positive" }
    require(maxCachedPages >= initialLoadPages + 1) {
        "maxCachedPages must retain more than the initial window"
    }
    val prefetchDistance = (pageSize / 2).coerceAtLeast(1)
    return PagingConfig(
        pageSize = pageSize,
        initialLoadSize = pageSize * initialLoadPages,
        prefetchDistance = prefetchDistance,
        enablePlaceholders = false,
        maxSize = pageSize * maxCachedPages,
    )
}
