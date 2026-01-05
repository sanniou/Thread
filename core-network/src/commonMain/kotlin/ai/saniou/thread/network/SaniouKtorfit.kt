package ai.saniou.thread.network

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
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
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun SaniouKtorfit(
    baseUrl: String,
    clientConfig: HttpClientConfig<*>.() -> Unit = {}
): Ktorfit = ktorfit {
    baseUrl(baseUrl)
    httpClient(
        HttpClient(HttpEngineFactory) {
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
                level = LogLevel.ALL
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
            clientConfig()
        }
    )
    converterFactories(
        FlowConverterFactory(),
        SaniouResponseConverterFactory(),
        WireConverterFactory()
    )
}
