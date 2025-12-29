package ai.saniou.thread.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A Ktor plugin that intercepts 403 Forbidden responses from Cloudflare
 * and triggers a challenge handling process (e.g., via WebView).
 */
class CloudflareProtectionPlugin(
    private val challengeHandler: ChallengeHandler,
    private val sourceId: String
) {
    // Used to prevent multiple concurrent challenge requests.
    // If multiple requests fail with 403 simultaneously, only one should trigger the UI.
    private val mutex = Mutex()
    // To track if a challenge is currently in progress or recently completed
    // This is a simplified approach; a more robust one might track validity time.
    // private var lastSuccessTime: Long = 0 

    class Config {
        var challengeHandler: ChallengeHandler? = null
        var sourceId: String = ""
    }

    companion object : HttpClientPlugin<Config, CloudflareProtectionPlugin> {
        override val key: AttributeKey<CloudflareProtectionPlugin> = AttributeKey("CloudflareProtectionPlugin")

        override fun prepare(block: Config.() -> Unit): CloudflareProtectionPlugin {
            val config = Config().apply(block)
            val handler = config.challengeHandler
                ?: throw IllegalArgumentException("ChallengeHandler must be provided")
            if (config.sourceId.isEmpty()) {
                throw IllegalArgumentException("sourceId must be provided")
            }
            return CloudflareProtectionPlugin(handler, config.sourceId)
        }

        override fun install(plugin: CloudflareProtectionPlugin, scope: HttpClient) {
            // Hook into the HttpSend plugin to intercept requests/responses
            scope.plugin(HttpSend).intercept { request ->
                // 1. Execute the original request
                val originalCall = execute(request)

                // 2. Check if it's a Cloudflare 403
                if (originalCall.response.status == HttpStatusCode.Forbidden &&
                    originalCall.response.headers["Server"]?.equals("cloudflare", ignoreCase = true) == true
                ) {
                    // It is a Cloudflare 403.
                    // We need to trigger the challenge handler.
                    
                    // We use a mutex to ensure we don't spam the UI with multiple challenge requests.
                    // If a challenge is already in progress, other requests will wait here.
                    val success = plugin.mutex.withLock {
                         // Double check inside lock?
                         // In a more complex scenario, we might check if cookies were updated recently.
                         // For now, we always ask the handler. The handler (UI) can decide to skip
                         // if it thinks it's already valid, but usually 403 means invalid.
                         
                         plugin.challengeHandler.handleChallenge(plugin.sourceId, request.url.toString())
                    }

                    if (success) {
                        // Challenge passed. Cookies should have been updated by the handler.
                        // Retry the request.
                        // We need to clone the request builder because it might have been modified/consumed?
                        // Actually, 'request' is an HttpRequestBuilder. 
                        // We can just create a new one copying from the original.
                        
                        val retryRequest = HttpRequestBuilder()
                        retryRequest.takeFrom(request)
                        
                        // Close the original response to release resources
                        // originalCall.response.cancel() // Or just let it be closed by the new call?
                        // It's safer to not close it here if we want to return it in failure case, 
                        // but since we are retrying, we can effectively discard the old one.
                        
                        return@intercept execute(retryRequest)
                    }
                }

                // If not 403 or challenge failed, return the original response
                originalCall
            }
        }
    }
}