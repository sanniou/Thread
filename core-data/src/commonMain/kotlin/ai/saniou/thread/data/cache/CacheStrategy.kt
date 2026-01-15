package ai.saniou.thread.data.cache

import ai.saniou.thread.db.Database
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * 统一的缓存策略工具
 */
object CacheStrategy {

    /**
     * 检查是否需要更新数据
     *
     * @param db 数据库实例
     * @param keyType 远程键类型 (String)
     * @param keyId 远程键ID (targetId)
     * @param expiration 过期时间，默认为1天
     * @return 如果需要更新则返回 true，否则返回 false
     */
    fun shouldFetch(
        db: Database,
        keyType: String,
        keyId: String,
        expiration: Duration = 1.days
    ): Boolean {
        val now = Clock.System.now()
        val lastQueryTime = db.remoteKeyQueries.remoteKeyByTargetId(
            targetId = keyId,
            type = keyType
        ).executeAsOneOrNull()?.lastUpdated ?: 0L

        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastQueryTime)
        return now - lastUpdateInstant >= expiration
    }

    /**
     * 更新最后获取时间
     *
     * @param db 数据库实例
     * @param keyType 远程键类型 (String)
     * @param keyId 远程键ID (targetId)
     */
    suspend fun updateLastFetchTime(
        db: Database,
        keyType: String,
        keyId: String
    ) {
        val now = Clock.System.now()
        db.remoteKeyQueries.insertOrReplace(
            targetId = keyId,
            type = keyType,
            prevKey = null,
            nextKey = null,
            lastUpdated = now.toEpochMilliseconds(),
        )
    }
}
