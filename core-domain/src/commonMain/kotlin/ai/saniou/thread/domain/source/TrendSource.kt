package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab

interface TrendSource {
    /**
     * The unique identifier for the source (e.g., "nmb", "tieba").
     * This should match the parent Source's ID.
     */
    val id: String

    /**
     * The display name for the source (e.g., "A岛", "贴吧").
     */
    val name: String

    /**
     * Returns a list of supported trend tabs for this source.
     * This will be called to dynamically build the UI.
     */
    fun getTrendTabs(): List<TrendTab>

    /**
     * Fetches trend data for a given tab and page.
     * This is used by the RemoteMediator to fetch data from the network.
     *
     * @param cursor The pagination cursor. For page-based sources, this is the page number (as String).
     *               For time-based sources, this is the timestamp.
     */
    suspend fun fetchTrendData(
        tab: TrendTab,
        params: TrendParams,
        cursor: String?,
    ): Result<List<TrendItem>> {
        // Default implementation for backward compatibility with page-based sources
        return fetchTrendData(tab, params, cursor?.toIntOrNull() ?: 1)
    }

    @Deprecated("Use fetchTrendData(tab, params, cursor) instead")
    suspend fun fetchTrendData(
        tab: TrendTab,
        params: TrendParams,
        page: Int,
    ): Result<List<TrendItem>> = Result.success(emptyList())

    fun trendDataEnded(tab: TrendTab, params: TrendParams, trends: List<TrendItem>): Boolean
}
