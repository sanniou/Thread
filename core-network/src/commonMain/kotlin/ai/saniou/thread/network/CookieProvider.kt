package ai.saniou.thread.network

/**
 * 提供动态 Cookie 的抽象接口
 */
fun interface CookieProvider {
    /**
     * 获取要添加到 Header 的 Cookie 值
     */
    suspend fun getCookieValue(): String?
}