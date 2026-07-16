package ai.saniou.thread.data.sync.webdav

import ai.saniou.thread.domain.model.sync.WebDavConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64

interface UserDataRemoteTransport {
    suspend fun write(config: WebDavConfig, payload: String): Result<Unit>
    suspend fun read(config: WebDavConfig): Result<String>
}

class WebDavSyncTransport(
    private val httpClient: HttpClient,
) : UserDataRemoteTransport {
    override suspend fun write(config: WebDavConfig, payload: String): Result<Unit> = runCatching {
        val response = httpClient.put(config.endpoint) {
            applyAuthorization(config)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        check(response.status.isSuccess()) {
            "WebDAV PUT failed: ${response.status.value} ${response.bodyAsText().take(200)}"
        }
    }

    override suspend fun read(config: WebDavConfig): Result<String> = runCatching {
        val response = httpClient.get(config.endpoint) { applyAuthorization(config) }
        check(response.status.isSuccess()) {
            "WebDAV GET failed: ${response.status.value} ${response.bodyAsText().take(200)}"
        }
        response.bodyAsText()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuthorization(config: WebDavConfig) {
        if (config.username.isNotBlank()) {
            val credentials = Base64.Default.encode("${config.username}:${config.password}".encodeToByteArray())
            header(HttpHeaders.Authorization, "Basic $credentials")
        }
    }
}
