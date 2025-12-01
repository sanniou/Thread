package ai.saniou.corecommon.data

import io.ktor.client.plugins.api.createClientPlugin

/**
 * 动态添加 Cookie 的 Ktor 插件
 */
val DynamicCookiePlugin = createClientPlugin("DynamicCookiePlugin", ::PluginConfiguration) {
    val cookieProvider = pluginConfig.cookieProvider

    onRequest { request, _ ->
        // 仅对特定的 host 添加 Cookie
        if (request.url.host == "api.nmb.best" || request.url.host == "www.nmbxd1.com" || request.url.host == "www.nmbxd.com") {
            cookieProvider?.getCookieValue()?.let {
                request.headers.append("Cookie", "userhash=$it")
            }
        }
    }
}

class PluginConfiguration {
    var cookieProvider: CookieProvider? = null
}
