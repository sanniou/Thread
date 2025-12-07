package ai.saniou.nmb.data.repository

/**
 * 数据加载策略
 */
enum class DataPolicy {
    /**
     * 缓存优先
     *
     * 如果数据库中存在数据，则直接返回，不访问网络。
     * 适用于数据不经常变化的场景，如帖子回复。
     */
    CACHE_FIRST,

    /**
     * API 优先
     *
     * 强制从网络获取最新数据，并更新本地数据库。
     * 适用于数据流频繁变化的场景，如板块帖子列表。
     */
    API_FIRST
}