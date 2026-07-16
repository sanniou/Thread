package ai.saniou.thread.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/** Stable HTTP failure passed through connectors, retry policy, diagnostics, and UI. */
class HttpStatusException(
    val statusCode: Int,
    val responseBody: String,
    val retryAfterSeconds: Long? = null,
) : RuntimeException(
    buildString {
        append("HTTP ")
        append(statusCode)
        retryAfterSeconds?.let { append(" retry-after=").append(it).append('s') }
        responseBody.toDiagnosticSummary().takeIf(String::isNotBlank)?.let {
            append(": ").append(it)
        }
    }
)

internal suspend fun HttpResponse.toHttpStatusException(): HttpStatusException {
    val body = runCatching { bodyAsText() }.getOrDefault("")
    return HttpStatusException(
        statusCode = status.value,
        responseBody = body,
        retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
    )
}

private fun String.toDiagnosticSummary(): String =
    replace(Regex("\\s+"), " ").trim().take(MAX_DIAGNOSTIC_BODY)

private const val MAX_DIAGNOSTIC_BODY = 512
