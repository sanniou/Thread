package ai.saniou.thread.data.manager

import ai.saniou.thread.network.CookieProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages cookies for network requests.
 * Allows updating cookies from UI (e.g., WebView after CF challenge) and providing them to API clients.
 */
class CookieManager : CookieProvider {
    private val _cookieValue = MutableStateFlow<String?>(null)
    val cookieValue = _cookieValue.asStateFlow()
    private val mutex = Mutex()

    /**
     * Updates the cookie string.
     * @param cookies The full cookie string (e.g., "key1=value1; key2=value2")
     */
    suspend fun updateCookies(cookies: String) {
        mutex.withLock {
            _cookieValue.value = cookies
        }
    }

    /**
     * Appends or updates specific cookies.
     * This is a simple implementation that just overwrites for now, or could be smarter.
     */
    suspend fun setCookie(key: String, value: String) {
        mutex.withLock {
            val current = _cookieValue.value ?: ""
            // Simple append for now, proper parsing would be better but complex without HttpCookie
            val newCookie = "$key=$value"
            if (current.isEmpty()) {
                _cookieValue.value = newCookie
            } else {
                 // Very basic replacement/append logic
                 if (current.contains(key)) {
                     // Regex replace
                     _cookieValue.value = current.replace(Regex("$key=[^;]*"), "$key=$value")
                 } else {
                     _cookieValue.value = "$current; $newCookie"
                 }
            }
        }
    }

    override suspend fun getCookieValue(): String? {
        return _cookieValue.value
    }
}