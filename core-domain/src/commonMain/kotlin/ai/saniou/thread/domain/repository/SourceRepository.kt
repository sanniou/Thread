package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.feed.AggregatedFeedPage


/**
 * 信息流仓库接口，定义了领域层需要的数据操作
 */
interface SourceRepository {
    /**
     * 获取聚合的信息流
     * @param page 页码
     */
    suspend fun getAggregatedFeed(
        page: Int,
        sourceIds: Set<String>? = null,
    ): Result<AggregatedFeedPage>

    /**
     * 获取所有可用的信息源
     */
    fun getAvailableSources(): List<Source>

    /**
     * 获取指定 ID 的 Source
     */
    fun getSource(sourceId: String): Source?
}
