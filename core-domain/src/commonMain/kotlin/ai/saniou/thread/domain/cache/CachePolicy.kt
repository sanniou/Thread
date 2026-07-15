package ai.saniou.thread.domain.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class CacheResource {
    CHANNEL_CATALOG,
    TOPIC_DETAIL,
    TOPIC_COMMENTS,
    READER_FEED,
}

data class CachePolicy(
    val freshness: Duration,
    val serveStaleOnFailure: Boolean = true,
)

interface CachePolicyProvider {
    fun policy(sourceId: String, resource: CacheResource): CachePolicy
}

class DefaultCachePolicyProvider : CachePolicyProvider {
    override fun policy(sourceId: String, resource: CacheResource): CachePolicy = when (resource) {
        CacheResource.CHANNEL_CATALOG -> CachePolicy(12.hours)
        CacheResource.TOPIC_DETAIL -> CachePolicy(15.minutes)
        CacheResource.TOPIC_COMMENTS -> CachePolicy(5.minutes)
        CacheResource.READER_FEED -> CachePolicy(30.minutes)
    }
}
