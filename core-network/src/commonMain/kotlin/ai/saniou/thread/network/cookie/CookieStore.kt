package ai.saniou.thread.network.cookie

/**
 * 统一的 Cookie 存储接口，支持多 Source 隔离
 */
interface CookieStore {
    /**
     * 获取指定 Source 的 Cookie 字符串
     * @param sourceId Source ID (e.g., "discourse", "nmb")
     */
    suspend fun getCookie(sourceId: String): String?

    /**
     * 保存指定 Source 的 Cookie
     * @param sourceId Source ID
     * @param cookie Cookie 字符串
     */
    suspend fun saveCookie(sourceId: String, cookie: String)

    /**
     * 清除指定 Source 的 Cookie
     * @param sourceId Source ID
     */
    suspend fun clearCookie(sourceId: String)
}