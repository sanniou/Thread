package ai.saniou.thread.network

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.ContentEncodingConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun SaniouKtorfit(
    baseUrl: String,
    clientConfig: HttpClientConfig<*>.() -> Unit = {}
): Ktorfit = SaniouKtorfit(
    baseUrl = baseUrl,
    httpClient = SaniouHttpClient(clientConfig),
)

/** Injectable client boundary used by runtime connectors and recorded contract fixtures. */
fun SaniouKtorfit(baseUrl: String, httpClient: HttpClient): Ktorfit = ktorfit {
    baseUrl(baseUrl)
    httpClient(httpClient)
    converterFactories(
        FlowConverterFactory(),
        SaniouResponseConverterFactory(),
        WireConverterFactory()
    )
}

fun SaniouHttpClient(
    clientConfig: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(HttpEngineFactory) {
    installSaniouDefaults()
    clientConfig()
}

/** Allows JVM contract tests to run the exact production client against Ktor MockEngine. */
fun SaniouHttpClient(
    engine: HttpClientEngine,
    clientConfig: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(engine) {
    installSaniouDefaults()
    clientConfig()
}

private fun HttpClientConfig<*>.installSaniouDefaults() {
    install(ContentEncoding) {
        mode = ContentEncodingConfig.Mode.All
        gzip()
        deflate()
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
        val wireConverter = WireContentConverter()
        register(ContentType.Application.ProtoBuf, wireConverter)
        register(ContentType.Application.OctetStream, wireConverter)
    }
    install(DefaultRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
    install(Logging) {
        format = LoggingFormat.OkHttp
        logger = object : Logger {
            override fun log(message: String) {
                println(message)
            }
        }
        level = LogLevel.INFO
        sanitizeHeader { header ->
            header == HttpHeaders.Authorization ||
                header == HttpHeaders.Cookie ||
                header == HttpHeaders.SetCookie ||
                header.equals("User-Api-Key", ignoreCase = true)
        }
    }
}
