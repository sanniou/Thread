package ai.saniou.thread.network

import io.ktor.client.plugins.api.createClientPlugin

/**
 * 动态添加 Cookie 的 Ktor 插件
 */
val DynamicCookiePlugin = createClientPlugin("DynamicCookiePlugin", ::PluginConfiguration) {
    val cookieProvider = pluginConfig.cookieProvider

    onRequest { request, _ ->
        cookieProvider?.getCookieValue()?.let {
            request.headers.append("Cookie", it)
        }
    }
}

class PluginConfiguration {
    var cookieProvider: CookieProvider? = null
}