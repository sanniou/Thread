package ai.saniou.thread.network.cookie

import ai.saniou.thread.network.CookieProvider

/**
 * 适配器：将 Source ID 和 CookieStore 桥接为 CookieProvider
 */
class SourceCookieProvider(
    private val sourceId: String,
    private val cookieStore: CookieStore
) : CookieProvider {
    override suspend fun getCookieValue(): String? {
        return cookieStore.getCookie(sourceId)
    }
}