package ai.saniou.thread.data.contract

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

data class FixtureExchange(
    val method: HttpMethod,
    val path: String,
    val status: Int,
    val bodyResource: String,
    val responseHeaders: Map<String, String> = emptyMap(),
)

data class RecordedConnectorRequest(
    val method: String,
    val path: String,
    val query: String,
    val headerNames: Set<String>,
)

/**
 * Replayable HTTP boundary for connector contract tests. Response bodies live as checked-in
 * resources, while requests are recorded without credential values for regression assertions.
 */
class ConnectorContractHarness(vararg exchanges: FixtureExchange) {
    private val pending = exchanges.toMutableList()
    private val mutableRequests = mutableListOf<RecordedConnectorRequest>()

    val requests: List<RecordedConnectorRequest>
        get() = synchronized(mutableRequests) { mutableRequests.toList() }

    val engine = MockEngine { request ->
        val exchange = synchronized(pending) {
            val index = pending.indexOfFirst {
                it.method == request.method && it.path == request.url.encodedPath
            }
            check(index >= 0) {
                "Unexpected connector request ${request.method.value} ${request.url.encodedPath}"
            }
            pending.removeAt(index)
        }
        synchronized(mutableRequests) {
            mutableRequests += RecordedConnectorRequest(
                method = request.method.value,
                path = request.url.encodedPath,
                query = request.url.encodedQuery,
                headerNames = request.headers.names(),
            )
        }
        respond(
            content = fixture(exchange.bodyResource),
            status = HttpStatusCode.fromValue(exchange.status),
            headers = Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                exchange.responseHeaders.forEach { (name, value) -> append(name, value) }
            },
        )
    }

    fun assertExhausted() {
        check(pending.isEmpty()) {
            "Unconsumed connector fixtures: ${pending.joinToString { "${it.method.value} ${it.path}" }}"
        }
    }

    private fun fixture(path: String): String {
        val resource = checkNotNull(javaClass.classLoader.getResource("connector-fixtures/$path")) {
            "Missing connector fixture '$path'"
        }
        return resource.readText()
    }
}
