package ai.saniou.thread.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin

/**
 * 极简的 Cookie 注入扩展。
 * 在请求发送前，执行 [getCookie] 并将其结果作为 Cookie 头添加到请求中。
 */
fun HttpClientConfig<*>.installCookieAuth(getCookie: suspend () -> String?) {
    install(createClientPlugin("SimpleCookieAuth") {
        onRequest { request, _ ->
            getCookie()?.let {
                request.headers.append("Cookie", it)
            }
        }
    })
}