package ai.saniou.thread.data.source.acfun.remote

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AcfunHeaderPluginConfig {
    var tokenManager: AcfunTokenManager? = null
}

val AcfunHeaderPlugin = createClientPlugin("AcfunHeaderPlugin", ::AcfunHeaderPluginConfig) {
    val tokenManager = pluginConfig.tokenManager ?: throw IllegalStateException("AcfunTokenManager is required")

    onRequest { request, _ ->
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val year = now.year
        val month = now.monthNumber.toString().padStart(2, '0')
        val day = now.dayOfMonth.toString().padStart(2, '0')
        val hour = now.hour.toString().padStart(2, '0')
        val minute = now.minute.toString().padStart(2, '0')
        val second = now.second.toString().padStart(2, '0')
        val millis = (now.nanosecond / 1_000_000).toString().padStart(3, '0')
        val requestTime = "$year-$month-$day $hour:$minute:$second.$millis"

        val udid = tokenManager.getUdid()
        val cookie = tokenManager.getCookie()
        val token = tokenManager.getToken()
        val acSecurity = tokenManager.getAcSecurity()

        request.header("User-Agent", "acvideo core/6.31.1.1026(OPPO;OPPO A83;7.1.1)")
        request.header("deviceType", "1")
        request.header("productId", "2000")
        request.header("market", "tencent")
        request.header("udid", udid)
        request.header("resolution", "1080x1920")
        request.header("net", "WIFI")
        request.header("requestTime", requestTime)
        request.header("Content-Type", "application/x-www-form-urlencoded")

        // Cookie handling:
        // If logged in, `cookie` in TokenManager should contain the full cookie string (including did, tokens, etc.)
        // mimicking QML's `makeCookie` result.
        // If not logged in, we provide a basic cookie with `did` and `safety_id`.
        if (!cookie.isNullOrBlank()) {
            request.header("Cookie", cookie)
        } else {
            request.header("Cookie", "did=$udid;safety_id=AAFAsQ04RM6Acm0WUcbfyJ5Q")
        }

        if (token != null) {
            request.header("access_token", token)
        }
        if (acSecurity != null) {
            request.header("token", acSecurity)
        }
    }
}
