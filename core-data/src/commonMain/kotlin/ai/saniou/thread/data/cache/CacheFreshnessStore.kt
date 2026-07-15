package ai.saniou.thread.data.cache

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.cache.CachePolicy
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class CacheFreshnessStore(
    private val database: Database,
) {
    suspend fun isFresh(key: String, policy: CachePolicy): Boolean = withContext(ioDispatcher) {
        val lastUpdated = database.remoteKeyQueries.remoteKeyByTargetId(
            targetId = key,
            type = TYPE,
        ).executeAsOneOrNull()?.lastUpdated ?: return@withContext false
        Clock.System.now() - Instant.fromEpochMilliseconds(lastUpdated) < policy.freshness
    }

    suspend fun markFresh(key: String) = withContext(ioDispatcher) {
        database.remoteKeyQueries.insertOrReplace(
            targetId = key,
            type = TYPE,
            prevKey = null,
            nextKey = null,
            lastUpdated = Clock.System.now().toEpochMilliseconds(),
        )
    }

    suspend fun invalidate(key: String) = withContext(ioDispatcher) {
        database.remoteKeyQueries.deleteByTargetIdAndType(key, TYPE)
    }

    companion object {
        const val TYPE = "cache_freshness"
        fun channelCatalog(sourceId: String) = "source:$sourceId:channels"
        fun topic(sourceId: String, topicId: String) = "source:$sourceId:topic:$topicId"
        fun comments(sourceId: String, topicId: String) = "source:$sourceId:comments:$topicId"
        fun reader(feedId: String) = "reader:$feedId"
    }
}
